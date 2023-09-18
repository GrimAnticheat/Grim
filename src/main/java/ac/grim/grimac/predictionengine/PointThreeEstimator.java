package ac.grim.grimac.predictionengine;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.predictions.PredictionEngine;
import ac.grim.grimac.utils.collisions.CollisionData;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.nmsutil.*;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.potion.PotionType;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.util.Vector;

import java.util.Set;

/**
 * A lot of care was put into handling all the stupid stuff occurring between events
 * <p>
 * Such as: Placing water and lava in a worldguard region to climb up walls within 0.03
 * A single tick of bubble columns
 * Placing ladders in worldguard regions
 * Some plugin thinking it's funny to spam levitation effects rapidly
 * Some plugin thinking it's funny to spam gravity effects rapidly
 * Someone trying to false grim by using negative levitation effects
 * Open trapdoor, 0.03 upward into closed trapdoor, open trapdoor the tick before the next movement.
 * <p>
 * We must separate horizontal and vertical movement
 * The player can never actually control vertical movement directly
 * Vertically - we must compensate for gravity and for stepping movement
 * <p>
 * Stepping can be compensated for by expanding by 0.03, seting the vector down by the minimum movement allowed
 * and then moving the box up by the collision epsilon, and then pushing the box by 0.03 again
 * avoiding using the isEmpty() and rather using the collision move method, to avoid bypass/abuse
 * <p>
 * Jumping movement IS one of these starting vectors, although the length between the jump and
 * not jumping is outside the allowed vectors - as jumping cannot desync
 * <p>
 * Fluid pushing is quite strange - we simply expand by 0.03 and check for horizontal and vertical flowing.
 * As poses often desync, we cannot actually know the exact value.
 * <p>
 * Additionally, we must recheck for fluid between world updates to see if the player was swimming
 * or in lava at any point within the skipped tick
 * <p>
 * We must also check for a player starting gliding, stopping gliding, all within 0.03, which might
 * be possible due to mojang's implementation of gliding and netcode
 * <p>
 * We must also check for the user placing ladders, which gives them control of vertical movement
 * once again also between world changes
 * <p>
 * We must also be aware of sneaking, which is implemented terribly by mojang
 * There should be a post check for sending sneaking updates, but it's not implemented yet...
 * If the user has been sneaking for 2 movements without stopping, then we know that they are sneaking
 * This is due to poses being done AFTER the player moves, adding a 50 ms delay
 * And due to slowness processing BEFORE poses are updated, adding another 50 ms delay
 * However, on 1.13, the delay is instant because mojang wasn't given a chance to be incompetent -_-
 * <p>
 * We also must be aware of levitation from the last tick
 * We also must be aware of bubble columns
 * <p>
 * Additionally, because poses are done AFTER the previous tick, we must know the minimum height the player's
 * bounding box can be, to avoid noclip falses.  Funnily enough, vanilla falses due to this...
 * This is done because when the player can't have changed their pose for one tick, the second we know their god
 * damn pose.  The third tick fixes the slow movement desync.  Thanks a lot, mojang - for falsing
 * your own anticheat and not caring enough to fix it.  Causing this giant mess that we all know you won't
 * fix for another decade... and if you do fix it... you will only make it worse (remember the bucket desync?)
 * <p>
 * Call me out for the code (in this class) - but please put the blame on Mojang instead.  None of this would be needed
 * if Minecraft's netcode wasn't so terrible.
 * <p>
 * 1.18.2 fixes this issue.  However, this code must now be applied to tick skipping,
 * and I don't feel like writing another rant about tick skipping as mojang will never fix it, as it would
 * increase bandwidth usage.  At least it only causes falses occasionally, and not bypasses.
 */
public class PointThreeEstimator {
    private final GrimPlayer player;

    // The one thing we don't need to store is if the player 0.03'd to the ground, as this sends a packet
    // seriously, why mojang.  You send the player touched the ground but not their pos.
    // Is the position not important to you?  Why do you throw this data out??? God-damn it Mojang!
    //
    // If a player is moving upwards and a block is within 0.03 of their head, then they can hit this block
    // This results in what appears to be too great of gravity
    private boolean headHitter = false;
    // If the player was within 0.03 of water between now and the last movement
    public boolean isNearFluid = false;
    // If a player places a ladder in a worldguard region etc.
    @Getter
    private boolean isNearClimbable = false;
    // If a player stops and star gliding all within 0.03
    private boolean isGliding = false;
    // If the player's gravity has changed
    private boolean gravityChanged = false;

    private boolean isNearHorizontalFlowingLiquid = false; // We can't calculate the direction, only a toggle
    private boolean isNearVerticalFlowingLiquid = false; // We can't calculate exact values, once again a toggle
    private boolean isNearBubbleColumn = false; // We can't calculate exact values once again

    private int maxPositiveLevitation = Integer.MIN_VALUE; // Positive potion effects [0, 128]
    private int minNegativeLevitation = Integer.MAX_VALUE; // Negative potion effects [-127, -1]r

    @Setter
    @Getter
    private boolean isPushing = false;

    @Getter
    private boolean wasAlwaysCertain = true;

    public PointThreeEstimator(GrimPlayer player) {
        this.player = player;
    }

    // Handle game events that occur between skipped ticks - thanks a lot mojang for removing the idle packet!
    public void handleChangeBlock(int x, int y, int z, WrappedBlockState state) {
        CollisionBox data = CollisionData.getData(state.getType()).getMovementCollisionBox(player, player.getClientVersion(), state, x, y, z);
        SimpleCollisionBox normalBox = GetBoundingBox.getBoundingBoxFromPosAndSize(player.x, player.y, player.z, 0.6f, 1.8f);

        // Calculate head hitters.  Take a shortcut by checking if the player doesn't intersect with this block, but does
        // when the player vertically moves upwards by 0.03!  This is equivalent to the move method, but MUCH faster.
        SimpleCollisionBox slightlyExpanded = normalBox.copy().expand(0.03, 0, 0.03);
        if (!slightlyExpanded.isIntersected(data) && slightlyExpanded.offset(0, 0.03, 0).isIntersected(data)) {
            headHitter = true;
        }

        SimpleCollisionBox pointThreeBox = GetBoundingBox.getBoundingBoxFromPosAndSize(player.x, player.y - 0.03, player.z, 0.66f, 1.86f);
        if ((Materials.isWater(player.getClientVersion(), state) || state.getType() == StateTypes.LAVA) &&
                pointThreeBox.isIntersected(new SimpleCollisionBox(x, y, z))) {

            if (state.getType() == StateTypes.BUBBLE_COLUMN) {
                isNearBubbleColumn = true;
            }

            Vector fluidVector = FluidTypeFlowing.getFlow(player, x, y, z);
            if (fluidVector.getX() != 0 || fluidVector.getZ() != 0) {
                isNearHorizontalFlowingLiquid = true;
            }
            if (fluidVector.getY() != 0) {
                isNearVerticalFlowingLiquid = true;
            }

            isNearFluid = true;
        }

        if (pointThreeBox.isIntersected(new SimpleCollisionBox(x, y, z))) {
            // https://github.com/MWHunter/Grim/issues/613
            int controllingEntityId = player.compensatedEntities.getSelf().inVehicle() ? player.getRidingVehicleId() : player.entityID;
            player.firstBreadKB = player.checkManager.getKnockbackHandler().calculateFirstBreadKnockback(controllingEntityId, player.lastTransactionReceived.get());
            player.likelyKB = player.checkManager.getKnockbackHandler().calculateRequiredKB(controllingEntityId, player.lastTransactionReceived.get(), true);

            player.firstBreadExplosion = player.checkManager.getExplosionHandler().getFirstBreadAddedExplosion(player.lastTransactionReceived.get());
            player.likelyExplosions = player.checkManager.getExplosionHandler().getPossibleExplosions(player.lastTransactionReceived.get(), true);

            player.updateVelocityMovementSkipping();

            if (player.couldSkipTick) {
                player.uncertaintyHandler.lastPointThree.reset();
            }
        }

        if (!player.compensatedEntities.getSelf().inVehicle() && ((state.getType() == StateTypes.POWDER_SNOW && player.getInventory().getBoots().getType() == ItemTypes.LEATHER_BOOTS)
                || Materials.isClimbable(state.getType())) && pointThreeBox.isIntersected(new SimpleCollisionBox(x, y, z))) {
            isNearClimbable = true;
        }
    }

    /**
     * If a player's gravity changed, or they have levitation effects, it's safer to not predict their next gravity
     * and to just give them lenience
     */
    public boolean canPredictNextVerticalMovement() {
        return !gravityChanged && maxPositiveLevitation == Integer.MIN_VALUE && minNegativeLevitation == Integer.MAX_VALUE;
    }

    public double positiveLevitation(double y) {
        if (maxPositiveLevitation == Integer.MIN_VALUE) return y;
        return (0.05 * (maxPositiveLevitation + 1) - y * 0.2);
    }

    public double negativeLevitation(double y) {
        if (minNegativeLevitation == Integer.MAX_VALUE) return y;
        return (0.05 * (minNegativeLevitation + 1) - y * 0.2);
    }

    public boolean controlsVerticalMovement() {
        return isNearFluid || isNearClimbable || isNearHorizontalFlowingLiquid || isNearVerticalFlowingLiquid || isNearBubbleColumn || isGliding || player.uncertaintyHandler.influencedByBouncyBlock()
                || player.checkManager.getKnockbackHandler().isKnockbackPointThree() || player.checkManager.getExplosionHandler().isExplosionPointThree();
    }

    public void updatePlayerPotions(PotionType potion, Integer level) {
        if (potion == PotionTypes.LEVITATION) {
            maxPositiveLevitation = Math.max(level == null ? Integer.MIN_VALUE : level, maxPositiveLevitation);
            minNegativeLevitation = Math.min(level == null ? Integer.MAX_VALUE : level, minNegativeLevitation);
        }
    }

    public void updatePlayerGliding() {
        isGliding = true;
    }

    public void updatePlayerGravity() {
        gravityChanged = true;
    }

    public void endOfTickTick() {
        SimpleCollisionBox pointThreeBox = GetBoundingBox.getBoundingBoxFromPosAndSize(player.x, player.y - 0.03, player.z, 0.66f, 1.86f);

        // Determine the head hitter using the current Y position
        SimpleCollisionBox oldBB = player.boundingBox;

        headHitter = false;
        // Can we trust the pose height?
        for (float sizes : (player.skippedTickInActualMovement ? new float[]{0.6f, 1.5f, 1.8f} : new float[]{player.pose.height})) {
            // Try to limit collisions to be as small as possible, for maximum performance
            player.boundingBox = GetBoundingBox.getBoundingBoxFromPosAndSize(player.x, player.y + (sizes - 0.01f), player.z, 0.6f, 0.01f);
            headHitter = headHitter || Collisions.collide(player, 0, 0.03, 0).getY() != 0.03;
        }

        player.boundingBox = oldBB;

        checkNearbyBlocks(pointThreeBox);

        maxPositiveLevitation = Integer.MIN_VALUE;
        minNegativeLevitation = Integer.MAX_VALUE;

        isGliding = player.isGliding;
        gravityChanged = false;
        wasAlwaysCertain = true;
        isPushing = false;
    }

    private void checkNearbyBlocks(SimpleCollisionBox pointThreeBox) {
        // Reset variables
        isNearHorizontalFlowingLiquid = false;
        isNearVerticalFlowingLiquid = false;
        isNearClimbable = false;
        isNearBubbleColumn = false;
        isNearFluid = false;

        // Check for flowing water
        Collisions.hasMaterial(player, pointThreeBox, (pair) -> {
            WrappedBlockState state = pair.getFirst();
            if (Materials.isClimbable(state.getType()) || (state.getType() == StateTypes.POWDER_SNOW && !player.compensatedEntities.getSelf().inVehicle() && player.getInventory().getBoots().getType() == ItemTypes.LEATHER_BOOTS)) {
                isNearClimbable = true;
            }

            if (BlockTags.TRAPDOORS.contains(state.getType())) {
                isNearClimbable = isNearClimbable || Collisions.trapdoorUsableAsLadder(player, pair.getSecond().getX(), pair.getSecond().getY(), pair.getSecond().getZ(), state);
            }

            if (state.getType() == StateTypes.BUBBLE_COLUMN) {
                isNearBubbleColumn = true;
            }

            if (Materials.isWater(player.getClientVersion(), pair.getFirst()) || pair.getFirst().getType() == StateTypes.LAVA) {
                isNearFluid = true;
            }

            return false;
        });
    }

    public boolean closeEnoughToGroundToStepWithPointThree(VectorData data, double originalY) {
        if (player.compensatedEntities.getSelf().inVehicle()) return false; // No 0.03
        if (!player.isPointThree()) return false; // No 0.03

        // This is intensive, only run it if we need it... compensate for stepping with 0.03
        //
        // This is technically wrong
        // A player can 0.03 while stepping while slightly going off of the block, in order to not
        // be vertically colliding (for 1.14+ clients only)
        //
        // To that I say... how the do you even do that?
        // Yes, it's possible, but slightly going off mainly occurs when going at high speeds
        // and 0.03 when the player is barely moving
        //
        // This can cause falses in other parts of the anticheat, so it's better just to hope the
        // player doesn't step AND 0.03 AND step off at the same time... (even if they do, other
        // 0.03 mitigation systems MAY be able to fix this)
        //
        // I give up.
        //
        // TODO: Part of these bugs were due to stepping BB grabbing being wrong, not 0.03 - can we simplify this?
        if (player.clientControlledVerticalCollision && data != null && data.isZeroPointZeroThree()) {
            return checkForGround(originalY);
        }

        return false;
    }

    private boolean checkForGround(double y) {
        SimpleCollisionBox playerBox = player.boundingBox;
        player.boundingBox = player.boundingBox.copy().expand(0.03, 0, 0.03).offset(0, 0.03, 0);
        // 0.16 magic value -> 0.03 plus gravity, plus some additional lenience
        double searchDistance = -0.2 + Math.min(0, y);
        Vector collisionResult = Collisions.collide(player, 0, searchDistance, 0);
        player.boundingBox = playerBox;
        return collisionResult.getY() != searchDistance;
    }

    // This method can be improved by using the actual movement to see if 0.03 was feasible...
    public boolean determineCanSkipTick(float speed, Set<VectorData> init) {
        // If possible, check for idle packet
        // TODO: Find a better way to fix slime without forcing 0.03 where there is none
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9) && player.packetStateData.didLastMovementIncludePosition && !player.uncertaintyHandler.isSteppingOnSlime) {
            return false; // Last packet included a position so not 0.03
        }

        // Determine if the player can make an input below 0.03
        double minimum = Double.MAX_VALUE;

        if ((player.isGliding || player.wasGliding) && !player.packetStateData.didLastMovementIncludePosition) {
            return true;
        }

        // Thankfully vehicles don't have 0.03
        if (player.compensatedEntities.getSelf().inVehicle()) {
            return false;
        }

        if (isNearClimbable() || isPushing || player.uncertaintyHandler.wasAffectedByStuckSpeed() || player.compensatedFireworks.getMaxFireworksAppliedPossible() > 0) {
            return true;
        }

        boolean couldStep = player.isPointThree() && checkForGround(player.clientVelocity.getY());

        // Takes 0.01 millis, on average, to compute... this should be improved eventually
        for (VectorData data : init) {
            // Try to get the vector as close to zero as possible to give the best chance at 0.03...
            Vector toZeroVec = new PredictionEngine().handleStartingVelocityUncertainty(player, data, new Vector());
            // Collide to handle mostly gravity, but other scenarios similar to this.
            Vector collisionResult = Collisions.collide(player, toZeroVec.getX(), toZeroVec.getY(), toZeroVec.getZ(), Integer.MIN_VALUE, null);

            // If this tick is the tick after y velocity was by 0, a stepping movement is POSSIBLE to have been hidden
            // A bit hacky... is there a better way?  I'm unsure...
            boolean likelyStepSkip = player.isPointThree() && (data.vector.getY() > -0.08 && data.vector.getY() < 0.06) && couldStep;

            // We need to do hypot calculations for all 3 axis
            // sqrt(sqrt(x^2 + z^2)^2 + y^2) = hypot(x, z, y)
            double minHorizLength = Math.max(0, Math.hypot(collisionResult.getX(), collisionResult.getZ()) - speed);
            // Detection > 100% falseless for explosions and knockback... disappearing blocks below player is rare
            // plus we should be able to detect 0.03 with the other vectors anyways
            boolean forcedNo003 = data.isExplosion() || data.isKnockback();
            // If the player was last on the ground, then let's consider them to have not moved vertically
            // The world could have changed since the last tick causing a false
            double length = Math.hypot((!forcedNo003 && player.lastOnGround) || (likelyStepSkip || controlsVerticalMovement()) ? 0 : Math.abs(collisionResult.getY()), minHorizLength);

            minimum = Math.min(minimum, length);

            if (minimum < player.getMovementThreshold()) break;
        }

        // As long as we are mathematically correct here, this should be perfectly accurate
        return minimum < player.getMovementThreshold();
    }

    public double getHorizontalFluidPushingUncertainty(VectorData vector) {
        // We don't know if the player was in the water because of 0.03
        // End of tick and start of tick can double this fluid motion, so we need to double it
        return isNearHorizontalFlowingLiquid && vector.isZeroPointZeroThree() ? 0.014 * 2 : 0;
    }

    public double getVerticalFluidPushingUncertainty(VectorData vector) {
        // We don't know if the player was in the water because of 0.03
        // End of tick and start of tick can double this fluid motion, so we need to double it
        return (isNearBubbleColumn || isNearVerticalFlowingLiquid) && vector.isZeroPointZeroThree() ? 0.014 * 2 : 0;
    }

    public double getVerticalBubbleUncertainty(VectorData vectorData) {
        return isNearBubbleColumn && vectorData.isZeroPointZeroThree() ? 0.35 : 0;
    }

    public double getAdditionalVerticalUncertainty(VectorData vector) {
        double fluidAddition = vector.isZeroPointZeroThree() ? 0.014 : 0;

        if (player.compensatedEntities.getSelf().inVehicle()) return 0; // No 0.03

        if (headHitter) {
            wasAlwaysCertain = false;
            // Head hitters return the vector to 0, and then apply gravity to it.
            // Not much room for abuse for this, so keep it lenient
            return -Math.max(0, vector.vector.getY()) - 0.1 - fluidAddition;
        } else if (player.uncertaintyHandler.wasAffectedByStuckSpeed()) {
            wasAlwaysCertain = false;
            // This shouldn't be needed but stuck speed can desync very easily with 0.03...
            // Especially now that both sweet berries and cobwebs are affected by stuck speed and overwrite each other
            return -0.1 - fluidAddition;
        }

        // The player couldn't have skipped their Y tick here... no point to simulate (and stop a bypass)
        if (!vector.isZeroPointZeroThree()) return 0;

        double minMovement = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) ? 0.003 : 0.005;

        // This should likely be refactored, but it works well.
        double yVel = vector.vector.getY();
        double maxYTraveled = 0;
        boolean first = true;
        do {
            // If less than minimum movement, then set to 0
            if (Math.abs(yVel) < minMovement) yVel = 0;

            // Don't add the first vector to the movement.  We already counted it.
            if (!first) {
                maxYTraveled += yVel;
            }
            first = false;

            // Simulate end of tick vector
            yVel = iterateGravity(player, yVel);

            // We aren't making progress, avoid infinite loop (This can be due to the player not having gravity)
            if (yVel == 0) break;
        } while (Math.abs(maxYTraveled + vector.vector.getY()) < player.getMovementThreshold()); // Account for uncertainty, don't stop until we simulate past uncertainty point

        if (maxYTraveled != 0) {
            wasAlwaysCertain = false;
        }

        // Negate the current vector and replace it with the one we just simulated
        return maxYTraveled;
    }

    private double iterateGravity(GrimPlayer player, double y) {
        if (player.compensatedEntities.getLevitationAmplifier() != null) {
            // This supports both positive and negative levitation
            y += (0.05 * (player.compensatedEntities.getLevitationAmplifier() + 1) - y * 0.2);
        } else if (player.hasGravity) {
            // Simulate gravity
            y -= player.gravity;
        }

        // Simulate end of tick friction
        return y * 0.98;
    }
}
