package ac.grim.grimac.predictionengine;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntityStrider;
import ac.grim.grimac.utils.enums.EntityType;
import ac.grim.grimac.utils.lists.EvictingList;
import ac.grim.grimac.utils.nmsImplementations.GetBoundingBox;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.bukkit.block.BlockFace;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

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
    // Handles 0.03 vertical false where actual velocity is greater than predicted because of previous lenience
    public boolean wasLastGravityUncertain = false;
    // Marks how much to allow the actual velocity to deviate from predicted when
    // the previous lenience because of 0.03 would occur
    public double gravityUncertainty = 0;
    // The player landed while jumping but without new position information because of 0.03
    public boolean wasLastOnGroundUncertain = false;
    // Marks previous didGroundStatusChangeWithoutPositionPacket from last tick
    public boolean lastPacketWasGroundPacket = false;
    // Marks previous lastPacketWasGroundPacket from last tick
    public boolean lastLastPacketWasGroundPacket = false;
    // Slime sucks in terms of bouncing and stuff.  Trust client onGround when on slime
    public boolean isSteppingOnSlime = false;
    public boolean isSteppingOnIce = false;
    public boolean wasSteppingOnBouncyBlock = false;
    public boolean isSteppingOnBouncyBlock = false;
    public boolean isSteppingNearBubbleColumn = false;
    // Did the player claim to leave stuck speed? (0.03 messes these calculations up badly)
    public boolean claimingLeftStuckSpeed = false;
    public int stuckOnEdge = 0;
    public boolean nextTickScaffoldingOnEdge = false;
    public boolean scaffoldingOnEdge = false;
    // Marks whether the player could have landed but without position packet because 0.03
    public boolean lastTickWasNearGroundZeroPointZeroThree = false;
    // Give horizontal lenience if the previous movement was 0.03 because their velocity is unknown
    public boolean lastMovementWasZeroPointZeroThree = true;
    // Give horizontal lenience if two movements ago was 0.03 because especially on ice it matters
    public boolean lastLastMovementWasZeroPointZeroThree = false;
    // The player sent a ground packet in order to change their ground status
    public boolean didGroundStatusChangeWithoutPositionPacket = false;
    // How many entities are within 0.5 blocks of the player's bounding box?
    public EvictingList<Integer> collidingEntities = new EvictingList<>(3);
    public EvictingList<Double> pistonPushing = new EvictingList<>(20);
    public EvictingList<Boolean> flyingStatusSwitchHack = new EvictingList<>(5);
    public EvictingList<Boolean> glidingStatusSwitchHack = new EvictingList<>(6);
    public EvictingList<Boolean> legacyUnderwaterFlyingHack = new EvictingList<>(10);
    public EvictingList<Boolean> stuckMultiplierZeroPointZeroThree = new EvictingList<>(5);
    public EvictingList<Boolean> hardCollidingLerpingEntity = new EvictingList<>(3);
    // "Temporary" thirty million hard border workaround
    // There is nothing as permanent as temporary!!!
    // https://i.imgur.com/9pDMCKz.png
    public EvictingList<Boolean> thirtyMillionHardBorder = new EvictingList<>(3);
    public int lastTeleportTicks = 0;
    public int lastFlyingTicks = 0;
    public int lastSneakingChangeTicks = 0;
    public boolean hasSentValidMovementAfterTeleport = false;

    public UncertaintyHandler(GrimPlayer player) {
        this.player = player;
        reset();
    }

    public void reset() {
        pistonX = 0;
        pistonY = 0;
        pistonZ = 0;
        gravityUncertainty = 0;
        isStepMovement = false;
        slimePistonBounces = new HashSet<>();
    }

    public boolean countsAsZeroPointZeroThree(VectorData predicted) {
        // First tick movement should always be considered zero point zero three
        // Shifting movement is somewhat buggy because 0.03
        if (stuckOnEdge == -2 || wasAffectedByStuckSpeed() || influencedByBouncyBlock() || isSteppingNearBubbleColumn)
            return true;

        // Explicitly is 0.03 movement
        if (predicted.hasVectorType(VectorData.VectorType.ZeroPointZeroThree))
            return true;

        // Movement is too low to determine whether this is zero point zero three
        if (player.couldSkipTick && player.actualMovement.lengthSquared() < 0.01)
            return true;

        if ((lastFlyingTicks < 3) && Math.abs(predicted.vector.getY()) < 0.2 && predicted.vector.getY() != 0 && player.actualMovement.lengthSquared() < 0.2)
            return true;

        if (player.riptideSpinAttackTicks > 18)
            return true;

        return isSteppingOnIce && lastTickWasNearGroundZeroPointZeroThree && player.actualMovement.clone().setY(0).lengthSquared() < 0.01;
    }

    public boolean wasAffectedByStuckSpeed() {
        return !stuckMultiplierZeroPointZeroThree.isEmpty() && Collections.max(stuckMultiplierZeroPointZeroThree);
    }

    public boolean influencedByBouncyBlock() {
        return isSteppingOnBouncyBlock || wasSteppingOnBouncyBlock;
    }

    public double getOffsetHorizontal(VectorData data) {
        double pointThree = data.hasVectorType(VectorData.VectorType.ZeroPointZeroThree) ? 0.06 : lastMovementWasZeroPointZeroThree ? 0.06 : lastLastMovementWasZeroPointZeroThree ? 0.03 : 0;

        if (player.couldSkipTick && data.hasVectorType(VectorData.VectorType.Trident))
            pointThree = 0.06;

        if (wasAffectedByStuckSpeed())
            pointThree = 0.08;

        if (data.hasVectorType(VectorData.VectorType.ZeroPointZeroThree) && (influencedByBouncyBlock() || isSteppingOnIce))
            pointThree = 0.1;

        if (lastTeleportTicks > -3 || player.vehicleData.lastVehicleSwitch < 6 || stuckOnEdge > -3)
            pointThree = 0.1;

        if (player.uncertaintyHandler.claimingLeftStuckSpeed)
            pointThree = 0.15;

        if (Collections.max(thirtyMillionHardBorder))
            pointThree = 0.15;

        if (Collections.max(player.uncertaintyHandler.glidingStatusSwitchHack))
            pointThree = 0.15;

        if (player.uncertaintyHandler.scaffoldingOnEdge) {
            pointThree = Math.max(pointThree, player.speed * 1.6);
        }

        // 0.03 plus being able to maintain velocity even when shifting is brutal
        if (stuckOnEdge == -2 && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_14))
            pointThree = Math.max(pointThree, player.speed * 2);

        return pointThree;
    }

    public double getVerticalOffset(VectorData data) {
        // Not worth my time to fix this because checking flying generally sucks - if player was flying in last 2 ticks
        if ((lastFlyingTicks < 5) && Math.abs(data.vector.getY()) < (4.5 * player.flySpeed - 0.25))
            return 0.06;

        if (data.hasVectorType(VectorData.VectorType.ZeroPointZeroThree) && isSteppingNearBubbleColumn)
            return 0.35;

        // Debug output when bouncing on a bed with 0.03-like movement
        // [10:36:34 INFO]: [GrimAC] DefineOutside P: -1.3529602846240607E-4 -0.11397087614427903 -0.09891504315167055
        // [10:36:34 INFO]: [GrimAC] DefineOutside A: -1.3529602846240607E-4 -0.11397087614427903 -0.09891504315167055
        // [10:36:34 INFO]: [GrimAC] DefineOutside P: -6.764801675096521E-4 0.15 0.007984975003338945
        // [10:36:34 INFO]: [GrimAC] DefineOutside A: -6.764801675096521E-4 0.2542683097376681 0.007984975003338945
        if (data.hasVectorType(VectorData.VectorType.ZeroPointZeroThree) && influencedByBouncyBlock())
            return 0.28;

        if (Collections.max(thirtyMillionHardBorder))
            return 0.15;

        // Don't allow this uncertainty to be spoofed - use isActuallyOnGround
        // (Players control their onGround when this hack is active)
        if (Collections.max(player.uncertaintyHandler.glidingStatusSwitchHack) && !player.isActuallyOnGround)
            return 0.15;

        if (influencedByBouncyBlock() && Math.abs(player.actualMovement.getY()) < 0.2)
            return 0.1;

        if (player.couldSkipTick && data.hasVectorType(VectorData.VectorType.Trident))
            return 0.06;

        if (wasLastGravityUncertain)
            return 0.03;

        if (!controlsVerticalMovement())
            return 0;

        if (player.isSwimming && data.hasVectorType(VectorData.VectorType.ZeroPointZeroThree))
            return 0.15;

        return data.hasVectorType(VectorData.VectorType.ZeroPointZeroThree) ? 0.09 : lastMovementWasZeroPointZeroThree ? 0.06 : lastLastMovementWasZeroPointZeroThree ? 0.03 : 0;
    }

    public boolean controlsVerticalMovement() {
        return !player.hasGravity || player.wasTouchingWater || player.wasTouchingLava || influencedByBouncyBlock() || lastFlyingTicks < 3 || player.isGliding || player.isClimbing || player.lastWasClimbing != 0;
    }

    public boolean canSkipTick(List<VectorData> possibleVelocities) {
        // 0.03 is very bad with stuck speed multipliers
        if (player.inVehicle) {
            return false;
        } else if (wasAffectedByStuckSpeed()) {
            gravityUncertainty = -0.08;
            return true;
        } else if (player.wasTouchingLava || (influencedByBouncyBlock() && Math.abs(player.clientVelocity.getY()) < 0.2)) {
            return true;
        } else if (lastTickWasNearGroundZeroPointZeroThree && didGroundStatusChangeWithoutPositionPacket) {
            return true;
        } else {
            double threshold = player.uncertaintyHandler.getZeroPointZeroThreeThreshold();

            if (player.uncertaintyHandler.lastTickWasNearGroundZeroPointZeroThree) {
                for (VectorData data : possibleVelocities)
                    player.couldSkipTick = player.couldSkipTick || data.vector.getX() * data.vector.getX() + data.vector.getZ() * data.vector.getZ() < threshold;
            } else {
                for (VectorData data : possibleVelocities)
                    player.couldSkipTick = player.couldSkipTick || data.vector.lengthSquared() < threshold;
            }

            return player.couldSkipTick;
        }
    }

    // 0.04 is safe for speed 10, 0.03 is unsafe
    // 0.0016 is safe for speed 1, 0.09 is unsafe
    //
    // Taking these approximate values gives us this, the same 0.03 value for each speed
    // Don't give bonus for sprinting because sprinting against walls isn't possible
    public double getZeroPointZeroThreeThreshold() {
        return 0.096 * (player.speed / (player.isSprinting ? 1.3d : 1)) - 0.008;
    }

    public void checkForHardCollision() {
        // Look for boats the player could collide with
        SimpleCollisionBox expandedBB = player.boundingBox.copy().expandToCoordinate(player.clientVelocity.getX(), player.clientVelocity.getY(), player.clientVelocity.getZ()).expand(1);
        boolean hasHardCollision = false;

        findCollision:
        {
            for (PacketEntity entity : player.compensatedEntities.entityMap.values()) {
                if ((entity.type == EntityType.BOAT || entity.type == EntityType.SHULKER) && entity != player.playerVehicle) {
                    SimpleCollisionBox box = GetBoundingBox.getBoatBoundingBox(entity.position.getX(), entity.position.getY(), entity.position.getZ());
                    if (box.isIntersected(expandedBB)) {
                        hasHardCollision = true;
                        break findCollision;
                    }
                }
            }

            // Stiders can walk on top of other striders
            if (player.playerVehicle instanceof PacketEntityStrider) {
                for (Int2ObjectMap.Entry<PacketEntity> entityPair : player.compensatedEntities.entityMap.int2ObjectEntrySet()) {
                    PacketEntity entity = entityPair.getValue();
                    if (entity.type == EntityType.STRIDER && entity != player.playerVehicle && !entity.hasPassenger(entityPair.getIntKey())) {
                        SimpleCollisionBox box = GetBoundingBox.getPacketEntityBoundingBox(entity.position.getX(), entity.position.getY(), entity.position.getZ(), entity);
                        if (box.isIntersected(expandedBB)) {
                            hasHardCollision = true;
                            break findCollision;
                        }
                    }
                }
            }

            // Boats can collide with quite literally anything
            if (player.playerVehicle != null && player.playerVehicle.type == EntityType.BOAT) {
                for (Int2ObjectMap.Entry<PacketEntity> entityPair : player.compensatedEntities.entityMap.int2ObjectEntrySet()) {
                    PacketEntity entity = entityPair.getValue();
                    if (entity != player.playerVehicle && !entity.hasPassenger(entityPair.getIntKey())) {
                        SimpleCollisionBox box = GetBoundingBox.getPacketEntityBoundingBox(entity.position.getX(), entity.position.getY(), entity.position.getZ(), entity);
                        if (box.isIntersected(expandedBB)) {
                            hasHardCollision = true;
                            break findCollision;
                        }
                    }
                }
            }
        }

        player.uncertaintyHandler.hardCollidingLerpingEntity.add(hasHardCollision);
    }

    @Override
    public String toString() {
        return "UncertaintyHandler{" +
                "pistonX=" + pistonX +
                ", pistonY=" + pistonY +
                ", pistonZ=" + pistonZ +
                ", isStepMovement=" + isStepMovement +
                ", xNegativeUncertainty=" + xNegativeUncertainty +
                ", xPositiveUncertainty=" + xPositiveUncertainty +
                ", zNegativeUncertainty=" + zNegativeUncertainty +
                ", zPositiveUncertainty=" + zPositiveUncertainty +
                ", wasLastGravityUncertain=" + wasLastGravityUncertain +
                ", gravityUncertainty=" + gravityUncertainty +
                ", wasLastOnGroundUncertain=" + wasLastOnGroundUncertain +
                ", lastPacketWasGroundPacket=" + lastPacketWasGroundPacket +
                ", lastLastPacketWasGroundPacket=" + lastLastPacketWasGroundPacket +
                ", lastTickWasNearGroundZeroPointZeroThree=" + lastTickWasNearGroundZeroPointZeroThree +
                ", lastMovementWasZeroPointZeroThree=" + lastMovementWasZeroPointZeroThree +
                ", lastLastMovementWasZeroPointZeroThree=" + lastLastMovementWasZeroPointZeroThree +
                '}';
    }
}
