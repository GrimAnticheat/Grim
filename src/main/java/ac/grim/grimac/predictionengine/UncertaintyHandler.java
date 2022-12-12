package ac.grim.grimac.predictionengine;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.LastInstance;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntityRideable;
import ac.grim.grimac.utils.data.packetentity.PacketEntityStrider;
import ac.grim.grimac.utils.lists.EvictingQueue;
import ac.grim.grimac.utils.nmsutil.BoundingBoxSize;
import ac.grim.grimac.utils.nmsutil.ReachUtils;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import org.bukkit.util.Vector;

import java.util.*;

public class UncertaintyHandler {
    private final GrimPlayer player;
    // Handles uncertainty when a piston could have pushed a player in a direction
    // Only the required amount of uncertainty is given
    public EvictingQueue<Double> pistonX = new EvictingQueue<>(5);
    public EvictingQueue<Double> pistonY = new EvictingQueue<>(5);
    public EvictingQueue<Double> pistonZ = new EvictingQueue<>(5);
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
    // Slime sucks in terms of bouncing and stuff.  Trust client onGround when on slime
    public boolean isSteppingOnSlime = false;
    public boolean isSteppingOnIce = false;
    public boolean isSteppingOnHoney = false;
    public boolean wasSteppingOnBouncyBlock = false;
    public boolean isSteppingOnBouncyBlock = false;
    public boolean isSteppingNearBubbleColumn = false;
    public boolean isSteppingNearScaffolding = false;
    public boolean isSteppingNearShulker = false;
    public boolean isNearGlitchyBlock = false;
    public boolean isOrWasNearGlitchyBlock = false;
    // Did the player claim to leave stuck speed? (0.03 messes these calculations up badly)
    public boolean claimingLeftStuckSpeed = false;
    // Give horizontal lenience if the previous movement was 0.03 because their velocity is unknown
    public boolean lastMovementWasZeroPointZeroThree = false;
    // Give horizontal lenience if the last movement reset velocity because 0.03 becomes unknown then
    public boolean lastMovementWasUnknown003VectorReset = false;
    // Handles 0.03 vertical false where actual velocity is greater than predicted because of previous lenience
    public boolean wasZeroPointThreeVertically = false;
    // How many entities are within 0.5 blocks of the player's bounding box?
    public EvictingQueue<Integer> collidingEntities = new EvictingQueue<>(3);
    // Fishing rod pulling is another method of adding to a player's velocity
    public List<Integer> fishingRodPulls = new ArrayList<>();
    public SimpleCollisionBox fireworksBox = null;
    public SimpleCollisionBox fishingRodPullBox = null;

    public LastInstance lastFlyingTicks;
    public LastInstance lastFlyingStatusChange;
    public LastInstance lastUnderwaterFlyingHack;
    public LastInstance lastStuckSpeedMultiplier;
    public LastInstance lastHardCollidingLerpingEntity;
    public LastInstance lastThirtyMillionHardBorder;
    public LastInstance lastTeleportTicks;
    public LastInstance lastPointThree;
    public LastInstance stuckOnEdge;
    public LastInstance lastStuckNorth;
    public LastInstance lastStuckSouth;
    public LastInstance lastStuckWest;
    public LastInstance lastStuckEast;
    public LastInstance lastVehicleSwitch;
    public double lastHorizontalOffset = 0;
    public double lastVerticalOffset = 0;

    public UncertaintyHandler(GrimPlayer player) {
        this.player = player;
        this.lastFlyingTicks = new LastInstance(player);
        this.lastFlyingStatusChange = new LastInstance(player);
        this.lastUnderwaterFlyingHack = new LastInstance(player);
        this.lastStuckSpeedMultiplier = new LastInstance(player);
        this.lastHardCollidingLerpingEntity = new LastInstance(player);
        this.lastThirtyMillionHardBorder = new LastInstance(player);
        this.lastTeleportTicks = new LastInstance(player);
        this.lastPointThree = new LastInstance(player);
        this.stuckOnEdge = new LastInstance(player);
        this.lastStuckNorth = new LastInstance(player);
        this.lastStuckSouth = new LastInstance(player);
        this.lastStuckWest = new LastInstance(player);
        this.lastStuckEast = new LastInstance(player);
        this.lastVehicleSwitch = new LastInstance(player);
        tick();
    }

    public void tick() {
        pistonX.add(0d);
        pistonY.add(0d);
        pistonZ.add(0d);
        isStepMovement = false;

        isSteppingNearShulker = false;
        wasSteppingOnBouncyBlock = isSteppingOnBouncyBlock;
        isSteppingOnSlime = false;
        isSteppingOnBouncyBlock = false;
        isSteppingOnIce = false;
        isSteppingOnHoney = false;
        isSteppingNearBubbleColumn = false;
        isSteppingNearScaffolding = false;

        slimePistonBounces = new HashSet<>();
        tickFireworksBox();
    }

    public boolean wasAffectedByStuckSpeed() {
        return lastStuckSpeedMultiplier.hasOccurredSince(5);
    }

    public void tickFireworksBox() {
        fishingRodPullBox = fishingRodPulls.isEmpty() ? null : new SimpleCollisionBox();
        fireworksBox = null;

        for (int owner : fishingRodPulls) {
            PacketEntity entity = player.compensatedEntities.getEntity(owner);
            if (entity == null) continue;

            SimpleCollisionBox entityBox = entity.getPossibleCollisionBoxes();
            float width = BoundingBoxSize.getWidth(player, entity);
            float height = BoundingBoxSize.getHeight(player, entity);

            // Convert back to coordinates instead of hitbox
            entityBox.maxY -= height;
            entityBox.expand(-width / 2, 0, -width / 2);

            Vector maxLocation = new Vector(entityBox.maxX, entityBox.maxY, entityBox.maxZ);
            Vector minLocation = new Vector(entityBox.minX, entityBox.minY, entityBox.minZ);

            Vector diff = minLocation.subtract(new Vector(player.lastX, player.lastY + 0.8 * 1.8, player.lastZ)).multiply(0.1);
            fishingRodPullBox.minX = Math.min(0, diff.getX());
            fishingRodPullBox.minY = Math.min(0, diff.getY());
            fishingRodPullBox.minZ = Math.min(0, diff.getZ());

            diff = maxLocation.subtract(new Vector(player.lastX, player.lastY + 0.8 * 1.8, player.lastZ)).multiply(0.1);
            fishingRodPullBox.maxX = Math.max(0, diff.getX());
            fishingRodPullBox.maxY = Math.max(0, diff.getY());
            fishingRodPullBox.maxZ = Math.max(0, diff.getZ());
        }

        fishingRodPulls.clear();

        int maxFireworks = player.compensatedFireworks.getMaxFireworksAppliedPossible() * 2;
        if (maxFireworks <= 0 || (!player.isGliding && !player.wasGliding)) {
            return;
        }

        fireworksBox = new SimpleCollisionBox();

        Vector currentLook = ReachUtils.getLook(player, player.xRot, player.yRot);
        Vector lastLook = ReachUtils.getLook(player, player.lastXRot, player.lastYRot);

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

        // Reduce second tick uncertainty by minimum friction amount (if not velocity uncertainty)
        if (pointThree > threshold)
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


        return pointThree;
    }

    public boolean influencedByBouncyBlock() {
        return isSteppingOnBouncyBlock || wasSteppingOnBouncyBlock;
    }

    public double getVerticalOffset(VectorData data) {

        if (player.uncertaintyHandler.claimingLeftStuckSpeed)
            return 0.06;

        // We don't know if the player was pressing jump or not
        if (player.uncertaintyHandler.wasSteppingOnBouncyBlock && (player.wasTouchingWater || player.wasTouchingLava))
            return 0.06;

        // Not worth my time to fix this because checking flying generally sucks - if player was flying in last 2 ticks
        if ((lastFlyingTicks.hasOccurredSince(5)) && Math.abs(data.vector.getY()) < (4.5 * player.flySpeed - 0.25))
            return 0.06;

        double pointThree = player.getMovementThreshold();
        // This swim hop could be 0.03-influenced movement
        if (data.isTrident())
            return pointThree * 2;

        // Velocity resets velocity, so we only have to give 0.03 uncertainty rather than 0.06
        if (player.couldSkipTick && (data.isKnockback() || player.isClimbing) && !data.isZeroPointZeroThree())
            return pointThree;

        if (player.pointThreeEstimator.controlsVerticalMovement()) {
            // 0.03 from last tick into 0.03 now = 0.06 (could reduce by friction in the future, only 0.91 at most though)
            if (data.isZeroPointZeroThree() || lastMovementWasZeroPointZeroThree) return pointThree * 2;
        }

        // Handle the player landing on this tick or the next tick
        if (wasZeroPointThreeVertically || player.uncertaintyHandler.onGroundUncertain || player.uncertaintyHandler.lastPacketWasGroundPacket) return pointThree;

        return 0;
    }

    public double reduceOffset(double offset) {
        // Boats are too glitchy to check.
        // Yes, they have caused an insane amount of uncertainty!
        // Even 1 block offset reduction isn't enough... damn it mojang
        if (player.uncertaintyHandler.lastHardCollidingLerpingEntity.hasOccurredSince(3)) {
            offset -= 1.2;
        }

        if (player.uncertaintyHandler.isOrWasNearGlitchyBlock) {
            offset -= 0.25;
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
        if (hasHardCollision()) player.uncertaintyHandler.lastHardCollidingLerpingEntity.reset();
    }

    private boolean hasHardCollision() {
        // This bounding box can be infinitely large without crashing the server.
        // This works by the proof that if you collide with an object, you will stop near the object
        SimpleCollisionBox expandedBB = player.boundingBox.copy().expand(1);
        return isSteppingNearShulker || regularHardCollision(expandedBB) || striderCollision(expandedBB) || boatCollision(expandedBB);
    }

    private boolean regularHardCollision(SimpleCollisionBox expandedBB) {
        for (PacketEntity entity : player.compensatedEntities.entityMap.values()) {
            if ((EntityTypes.isTypeInstanceOf(entity.type, EntityTypes.BOAT) || entity.type == EntityTypes.SHULKER) && entity != player.compensatedEntities.getSelf().getRiding() &&
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
        if (player.compensatedEntities.getSelf().getRiding() != null && EntityTypes.isTypeInstanceOf(player.compensatedEntities.getSelf().getRiding().type, EntityTypes.BOAT)) {
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
