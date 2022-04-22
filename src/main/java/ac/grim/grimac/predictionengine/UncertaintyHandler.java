package ac.grim.grimac.predictionengine;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.predictions.PredictionEngineElytra;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntityRideable;
import ac.grim.grimac.utils.data.packetentity.PacketEntityStrider;
import ac.grim.grimac.utils.lists.EvictingList;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

public class UncertaintyHandler {
    private final GrimPlayer player;
    // Handles uncertainty when a piston could have pushed a player in a direction
    // Only the required amount of uncertainty is given
    public double pistonX;
    public double pistonY;
    public double pistonZ;
    // Did the player step onto a block?
    // This is needed because we don't know if a player jumped onto the step block or not
    // Jumping would set onGround to false while not would set it to true
    // Meaning no matter what, just trust the player's onGround status
    public boolean isStepMovement;
    // What directions could slime block pistons be pushing the player from
    public HashSet<BlockFace> slimePistonBounces;
    // Handles general uncertainty such as entity pushing and the 1.14+ X Z collision bug where X momentum is maintained
    public double xNegativeUncertainty = 0;
    public double xPositiveUncertainty = 0;
    public double zNegativeUncertainty = 0;
    public double zPositiveUncertainty = 0;
    public double yNegativeUncertainty = 0;
    public double yPositiveUncertainty = 0;
    // Slime block bouncing
    public double thisTickSlimeBlockUncertainty = 0;
    public double nextTickSlimeBlockUncertainty = 0;
    // The player landed while jumping but without new position information because of 0.03
    public boolean onGroundUncertain = false;
    // Marks previous didGroundStatusChangeWithoutPositionPacket from last tick
    public boolean lastPacketWasGroundPacket = false;
    // Marks previous lastPacketWasGroundPacket from last tick
    public boolean lastLastPacketWasGroundPacket = false;
    // Slime sucks in terms of bouncing and stuff.  Trust client onGround when on slime
    public boolean isSteppingOnSlime = false;
    public boolean isSteppingOnIce = false;
    public boolean isSteppingOnHoney = false;
    public boolean wasSteppingOnBouncyBlock = false;
    public boolean isSteppingOnBouncyBlock = false;
    public boolean isSteppingNearBubbleColumn = false;
    public boolean isNearGlitchyBlock = false;
    public boolean isOrWasNearGlitchyBlock = false;
    // Did the player claim to leave stuck speed? (0.03 messes these calculations up badly)
    public boolean claimingLeftStuckSpeed = false;
    public int stuckOnEdge = -100;
    public int lastStuckNorth = -100;
    public int lastStuckSouth = -100;
    public int lastStuckWest = -100;
    public int lastStuckEast = -100;
    public boolean nextTickScaffoldingOnEdge = false;
    public boolean scaffoldingOnEdge = false;
    // Give horizontal lenience if the previous movement was 0.03 because their velocity is unknown
    public boolean lastMovementWasZeroPointZeroThree = false;
    // Give horizontal lenience if the last movement reset velocity because 0.03 becomes unknown then
    public boolean lastMovementWasUnknown003VectorReset = false;
    // Handles 0.03 vertical false where actual velocity is greater than predicted because of previous lenience
    public boolean wasZeroPointThreeVertically = false;
    // Did the player change their look with elytra between tick (we can't calculate 0.03 here)
    public boolean claimedLookChangedBetweenTick = false;
    // How many entities are within 0.5 blocks of the player's bounding box?
    public EvictingList<Integer> collidingEntities = new EvictingList<>(3);
    public EvictingList<Double> pistonPushing = new EvictingList<>(20);
    public SimpleCollisionBox fireworksBox = null;

    public int lastFlyingTicks = -100;
    public int lastFlyingStatusChange = -100;
    public int lastUnderwaterFlyingHack = -100;
    public int lastStuckSpeedMultiplier = -100;
    public int lastHardCollidingLerpingEntity = -100;
    public int lastThirtyMillionHardBorder = -100;
    public int lastTeleportTicks = 0; // You spawn with a teleport

    public double lastHorizontalOffset = 0;
    public double lastVerticalOffset = 0;

    public UncertaintyHandler(GrimPlayer player) {
        this.player = player;
        tick();
    }

    public void tick() {
        pistonX = 0;
        pistonY = 0;
        pistonZ = 0;
        isStepMovement = false;
        slimePistonBounces = new HashSet<>();
        tickFireworksBox();
    }

    public boolean wasAffectedByStuckSpeed() {
        return lastStuckSpeedMultiplier > -5;
    }

    public void tickFireworksBox() {
        int maxFireworks = player.compensatedFireworks.getMaxFireworksAppliedPossible() * 2;

        if (maxFireworks <= 0 || (!player.isGliding && !player.wasGliding)) {
            fireworksBox = null;
            return;
        }

        Vector currentLook = PredictionEngineElytra.getVectorForRotation(player, player.yRot, player.xRot);
        Vector lastLook = PredictionEngineElytra.getVectorForRotation(player, player.lastYRot, player.lastXRot);

        double antiTickSkipping = player.isPointThree() ? 0 : 0.05; // With 0.03, let that handle tick skipping

        double minX = Math.min(-antiTickSkipping, currentLook.getX()) + Math.min(-antiTickSkipping, lastLook.getX());
        double minY = Math.min(-antiTickSkipping, currentLook.getY()) + Math.min(-antiTickSkipping, lastLook.getY());
        double minZ = Math.min(-antiTickSkipping, currentLook.getZ()) + Math.min(-antiTickSkipping, lastLook.getZ());
        double maxX = Math.max(antiTickSkipping, currentLook.getX()) + Math.max(antiTickSkipping, lastLook.getX());
        double maxY = Math.max(antiTickSkipping, currentLook.getY()) + Math.max(antiTickSkipping, lastLook.getY());
        double maxZ = Math.max(antiTickSkipping, currentLook.getZ()) + Math.max(antiTickSkipping, lastLook.getZ());

        minX *= 1.7;
        minY *= 1.7;
        minZ *= 1.7;
        maxX *= 1.7;
        maxY *= 1.7;
        maxZ *= 1.7;

        minX = Math.max(-1.7, minX);
        minY = Math.max(-1.7, minY);
        minZ = Math.max(-1.7, minZ);
        maxX = Math.min(1.7, maxX);
        maxY = Math.min(1.7, maxY);
        maxZ = Math.min(1.7, maxZ);

        // The maximum movement impact a firework can have is 1.7 blocks/tick
        // This scales with the look vector linearly
        fireworksBox = new SimpleCollisionBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public double getOffsetHorizontal(VectorData data) {
        double threshold = player.getMovementThreshold();

        boolean newVectorPointThree = player.couldSkipTick && data.isKnockback();
        boolean explicit003 = data.isZeroPointZeroThree() || lastMovementWasZeroPointZeroThree;
        boolean either003 = newVectorPointThree || explicit003;

        double pointThree = newVectorPointThree || lastMovementWasUnknown003VectorReset ? threshold : 0;

        // 0.91 * 0.6 * (offset * 2) = 0.03276 + 0.03 offset
        if (explicit003) {
            pointThree = 0.91 * 0.6 * (threshold * 2) + threshold;
        }

        // (offset * 2) * 0.91 * 0.8 = max + 0.03 offset
        if (either003 && (influencedByBouncyBlock() || isSteppingOnHoney))
            pointThree = 0.91 * 0.8 * (threshold * 2) + threshold;

        // (offset * 2) * 0.91 * 0.989 = max + 0.03 offset
        if (either003 && isSteppingOnIce)
            pointThree = 0.91 * 0.989 * (threshold * 2) + threshold;

        // Reduce second tick uncertainty by minimum friction amount
        if (!newVectorPointThree && either003)
            pointThree *= 0.91 * 0.989;

        // 0.06 * 0.91 = max + 0.03 offset
        if (either003 && (player.lastOnGround || player.isFlying))
            pointThree = 0.91 * (threshold * 2) + threshold;

        // Friction while gliding is 0.99 horizontally
        if (either003 && (player.isGliding || player.wasGliding)) {
            pointThree = (0.99 * (threshold * 2)) + threshold;
        }

        if (player.uncertaintyHandler.claimingLeftStuckSpeed)
            pointThree = 0.15;

        if (lastThirtyMillionHardBorder > -3)
            pointThree = 0.15;

        if (player.vehicleData.lastVehicleSwitch < 3)
            pointThree = Math.max(pointThree, player.speed * 2);

        if (player.uncertaintyHandler.scaffoldingOnEdge) {
            pointThree = Math.max(pointThree, player.speed * 1.6);
        }

        // 0.03 plus being able to maintain velocity even when shifting is brutal
        // Value patched - I have no idea why these things are different in liquid vs in air
        if (stuckOnEdge == ((player.wasTouchingWater || player.wasTouchingLava) ? 0 : -1)) {
            pointThree = Math.max(pointThree, player.speed * 2);
        }

        return pointThree;
    }

    public boolean influencedByBouncyBlock() {
        return isSteppingOnBouncyBlock || wasSteppingOnBouncyBlock;
    }

    public double getVerticalOffset(VectorData data) {
        if (lastThirtyMillionHardBorder > -3)
            return 0.15;

        if (player.uncertaintyHandler.claimingLeftStuckSpeed)
            return 0.06;

        if (player.vehicleData.lastVehicleSwitch < 3)
            return 0.1;

        // We don't know if the player was pressing jump or not
        if (player.uncertaintyHandler.wasSteppingOnBouncyBlock && (player.wasTouchingWater || player.wasTouchingLava))
            return 0.06;

        // Not worth my time to fix this because checking flying generally sucks - if player was flying in last 2 ticks
        if ((lastFlyingTicks < 5) && Math.abs(data.vector.getY()) < (4.5 * player.flySpeed - 0.25))
            return 0.06;

        double pointThree = player.getMovementThreshold();
        // This swim hop could be 0.03-influenced movement
        if (data.isTrident())
            return pointThree * 2;

        // Velocity resets velocity, so we only have to give 0.03 uncertainty rather than 0.06
        if (player.couldSkipTick && data.isKnockback())
            return pointThree;

        if (player.pointThreeEstimator.controlsVerticalMovement()) {
            // Yeah, the second 0.06 isn't mathematically correct but 0.03 messes everything up...
            // Water pushing, elytras, EVERYTHING vertical movement gets messed up.
            if (data.isZeroPointZeroThree()) return pointThree * 2;
            if (lastMovementWasZeroPointZeroThree) return pointThree * 2;
            if (wasZeroPointThreeVertically || player.uncertaintyHandler.lastPacketWasGroundPacket)
                return pointThree;
            return 0;
        }

        if (wasZeroPointThreeVertically || player.uncertaintyHandler.lastPacketWasGroundPacket)
            return pointThree;


        return 0;
    }

    public double reduceOffset(double offset) {
        // Exempt players from piston checks by giving them 1 block of lenience for any piston pushing
        if (Collections.max(player.uncertaintyHandler.pistonPushing) > 0) {
            offset -= 1;
        }

        // Boats are too glitchy to check.
        // Yes, they have caused an insane amount of uncertainty!
        // Even 1 block offset reduction isn't enough... damn it mojang
        if (player.uncertaintyHandler.lastHardCollidingLerpingEntity > -3) {
            offset -= 1.2;
        }

        if (player.uncertaintyHandler.isOrWasNearGlitchyBlock) {
            offset -= 0.25;
        }

        // Exempt flying status change
        if (player.uncertaintyHandler.lastFlyingStatusChange > -20) {
            offset = 0;
        }

        // This is a section where I hack around current issues with Grim itself...
        if (player.uncertaintyHandler.wasAffectedByStuckSpeed() && (!player.isPointThree() || player.compensatedEntities.getSelf().inVehicle())) {
            offset -= 0.01;
        }

        if (player.uncertaintyHandler.influencedByBouncyBlock() && (!player.isPointThree() || player.compensatedEntities.getSelf().inVehicle())) {
            offset -= 0.03;
        }
        // This is the end of that section.

        // I can't figure out how the client exactly tracks boost time
        if (player.compensatedEntities.getSelf().getRiding() instanceof PacketEntityRideable) {
            PacketEntityRideable vehicle = (PacketEntityRideable) player.compensatedEntities.getSelf().getRiding();
            if (vehicle.currentBoostTime < vehicle.boostTimeMax + 20)
                offset -= 0.01;
        }

        return Math.max(0, offset);
    }

    public void checkForHardCollision() {
        // Look for boats the player could collide with
        player.uncertaintyHandler.lastHardCollidingLerpingEntity--;
        if (hasHardCollision()) player.uncertaintyHandler.lastHardCollidingLerpingEntity = 0;
    }

    private boolean hasHardCollision() {
        // This bounding box can be infinitely large without crashing the server.
        // This works by the proof that if you collide with an object, you will stop near the object
        SimpleCollisionBox expandedBB = player.boundingBox.copy().expand(1);
        return regularHardCollision(expandedBB) || striderCollision(expandedBB) || boatCollision(expandedBB);
    }

    private boolean regularHardCollision(SimpleCollisionBox expandedBB) {
        for (PacketEntity entity : player.compensatedEntities.entityMap.values()) {
            if ((entity.type == EntityTypes.BOAT || entity.type == EntityTypes.SHULKER) && entity != player.compensatedEntities.getSelf().getRiding() &&
                    entity.getPossibleCollisionBoxes().isIntersected(expandedBB)) {
                return true;
            }
        }

        return false;
    }

    private boolean striderCollision(SimpleCollisionBox expandedBB) {
        // Stiders can walk on top of other striders
        if (player.compensatedEntities.getSelf().getRiding() instanceof PacketEntityStrider) {
            for (Map.Entry<Integer, PacketEntity> entityPair : player.compensatedEntities.entityMap.int2ObjectEntrySet()) {
                PacketEntity entity = entityPair.getValue();
                if (entity.type == EntityTypes.STRIDER && entity != player.compensatedEntities.getSelf().getRiding() && !entity.hasPassenger(entityPair.getValue())
                        && entity.getPossibleCollisionBoxes().isIntersected(expandedBB)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean boatCollision(SimpleCollisionBox expandedBB) {
        // Boats can collide with quite literally anything
        if (player.compensatedEntities.getSelf().getRiding() != null && player.compensatedEntities.getSelf().getRiding().type == EntityTypes.BOAT) {
            for (Map.Entry<Integer, PacketEntity> entityPair : player.compensatedEntities.entityMap.int2ObjectEntrySet()) {
                PacketEntity entity = entityPair.getValue();
                if (entity != player.compensatedEntities.getSelf().getRiding() && (player.compensatedEntities.getSelf().getRiding() == null || !player.compensatedEntities.getSelf().getRiding().hasPassenger(entityPair.getValue())) &&
                        entity.getPossibleCollisionBoxes().isIntersected(expandedBB)) {
                    return true;
                }
            }
        }

        return false;
    }
}
