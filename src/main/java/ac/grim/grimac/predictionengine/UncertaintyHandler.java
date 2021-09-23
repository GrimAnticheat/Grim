package ac.grim.grimac.predictionengine;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntityStrider;
import ac.grim.grimac.utils.enums.EntityType;
import ac.grim.grimac.utils.lists.EvictingList;
import ac.grim.grimac.utils.nmsImplementations.GetBoundingBox;
import org.bukkit.block.BlockFace;

import java.util.HashSet;
import java.util.List;
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
    // Handles 0.03 vertical false where actual velocity is greater than predicted because of previous lenience
    public boolean wasLastGravityUncertain = false;
    // Marks how much to allow the actual velocity to deviate from predicted when
    // the previous lenience because of 0.03 would occur
    public double gravityUncertainty = 0;
    public EvictingList<Double> slimeBlockUpwardsUncertainty = new EvictingList<>(2);
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
    public boolean isNearGlitchyBlock = false;
    // Did the player claim to leave stuck speed? (0.03 messes these calculations up badly)
    public boolean claimingLeftStuckSpeed = false;
    public int stuckOnEdge = -100;
    public int lastStuckNorth = -100;
    public int lastStuckSouth = -100;
    public int lastStuckWest = -100;
    public int lastStuckEast = -100;
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

    // Fireworks are pure uncertainty and cause issues (Their implementation is terrible)
    public boolean lastUsingFirework = false;
    public int lastFireworkStatusChange = -100;

    public int lastTeleportTicks = -100;
    public int lastFlyingTicks = -100;
    public int lastSneakingChangeTicks = -100;
    public int lastGlidingChangeTicks = -100;
    public int lastMetadataDesync = -100;
    public int lastFlyingStatusChange = -100;
    public int lastUnderwaterFlyingHack = -100;
    public int lastStuckSpeedMultiplier = -100;
    public int lastHardCollidingLerpingEntity = -100;
    public int lastThirtyMillionHardBorder = -100;

    public double lastHorizontalOffset = 0;
    public double lastVerticalOffset = 0;

    public UncertaintyHandler(GrimPlayer player) {
        this.player = player;

        // Add stuff to evicting list to avoid issues later on
        slimeBlockUpwardsUncertainty.add(0d);
        slimeBlockUpwardsUncertainty.add(0d);

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
        if (stuckOnEdge == -2 || wasAffectedByStuckSpeed() || isSteppingNearBubbleColumn)
            return true;

        // Explicitly is 0.03 movement
        if (predicted.isZeroPointZeroThree())
            return true;

        if (player.uncertaintyHandler.stuckOnEdge > -3)
            return true;

        // Uncertainty was given here for 0.03-influenced movement
        if (predicted.isSwimHop())
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
        return lastStuckSpeedMultiplier > -5;
    }

    public double getOffsetHorizontal(VectorData data) {
        boolean has003 = data.isZeroPointZeroThree();
        double pointThree = has003 ? 0.06 : lastMovementWasZeroPointZeroThree ? 0.03 : lastLastMovementWasZeroPointZeroThree ? 0.03 : 0;

        // Velocity resets velocity, so we only have to give 0.03 uncertainty rather than 0.06
        if (player.couldSkipTick && data.isKnockback())
            pointThree = 0.03;

        // This swim hop could be 0.03-influenced movement
        if (data.isSwimHop() || data.isTrident())
            pointThree = 0.06;

        if (has003 && (influencedByBouncyBlock() || isSteppingOnIce))
            pointThree = 0.1;

        if (lastTeleportTicks > -3 || player.vehicleData.lastVehicleSwitch < 6)
            pointThree = 0.1;

        if (player.uncertaintyHandler.claimingLeftStuckSpeed)
            pointThree = 0.15;

        if (lastThirtyMillionHardBorder > -3)
            pointThree = 0.15;
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
        boolean has003 = data.isZeroPointZeroThree();

        if (has003 && isSteppingNearBubbleColumn)
            return 0.35;

        if (lastThirtyMillionHardBorder > -3)
            return 0.15;

        if (player.uncertaintyHandler.claimingLeftStuckSpeed)
            return 0.06;

        if (player.vehicleData.lastVehicleSwitch < 8)
            return 0.06;

        // We don't know if the player was pressing jump or not
        if (player.uncertaintyHandler.wasSteppingOnBouncyBlock && (player.wasTouchingWater || player.wasTouchingLava))
            return 0.06;

        // Not worth my time to fix this because checking flying generally sucks - if player was flying in last 2 ticks
        if ((lastFlyingTicks < 5) && Math.abs(data.vector.getY()) < (4.5 * player.flySpeed - 0.25))
            return 0.06;

        // This swim hop could be 0.03-influenced movement
        if (data.isSwimHop() || data.isTrident())
            return 0.06;

        // Velocity resets velocity, so we only have to give 0.03 uncertainty rather than 0.06
        if (player.couldSkipTick && data.isKnockback())
            return 0.03;

        if (controlsVerticalMovement()) {
            return has003 ? 0.06 : lastMovementWasZeroPointZeroThree ? 0.03 : lastLastMovementWasZeroPointZeroThree || wasLastGravityUncertain || player.uncertaintyHandler.lastPacketWasGroundPacket ? 0.03 : 0;
        }

        if (wasLastGravityUncertain || player.uncertaintyHandler.lastPacketWasGroundPacket)
            return 0.03;

        return 0;
    }

    public boolean controlsVerticalMovement() {
        return !player.hasGravity || player.wasTouchingWater || player.wasTouchingLava || influencedByBouncyBlock() || lastFlyingTicks < 3 || player.isGliding || player.isClimbing || player.lastWasClimbing != 0;
    }

    public boolean canSkipTick(List<VectorData> possibleVelocities) {
        // Reset could skip tick
        player.couldSkipTick = false;

        // 0.03 is very bad with stuck speed multipliers
        if (player.inVehicle) {
            return false;
        } else if (wasAffectedByStuckSpeed()) {
            gravityUncertainty = -0.08;
            return true;
        } else if (player.wasTouchingLava) {
            return true;
        } else if (lastTickWasNearGroundZeroPointZeroThree && didGroundStatusChangeWithoutPositionPacket && player.clientVelocity.getY() < 0.03) {
            return true;
        } else {
            double threshold = player.uncertaintyHandler.getZeroPointZeroThreeThreshold();

            if (player.uncertaintyHandler.lastTickWasNearGroundZeroPointZeroThree && player.clientVelocity.getY() < 0.03) {
                for (VectorData data : possibleVelocities)
                    player.couldSkipTick = player.couldSkipTick || data.vector.getX() * data.vector.getX() + data.vector.getZ() * data.vector.getZ() < threshold && !data.isKnockback();
            } else {
                for (VectorData data : possibleVelocities)
                    player.couldSkipTick = player.couldSkipTick || data.vector.lengthSquared() < threshold && !data.isKnockback();
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
                for (Map.Entry<Integer, PacketEntity> entityPair : player.compensatedEntities.entityMap.entrySet()) {
                    PacketEntity entity = entityPair.getValue();
                    if (entity.type == EntityType.STRIDER && entity != player.playerVehicle && !entity.hasPassenger(entityPair.getKey())) {
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
                for (Map.Entry<Integer, PacketEntity> entityPair : player.compensatedEntities.entityMap.entrySet()) {
                    PacketEntity entity = entityPair.getValue();
                    if (entity != player.playerVehicle && !entity.hasPassenger(entityPair.getKey())) {
                        SimpleCollisionBox box = GetBoundingBox.getPacketEntityBoundingBox(entity.position.getX(), entity.position.getY(), entity.position.getZ(), entity);
                        if (box.isIntersected(expandedBB)) {
                            hasHardCollision = true;
                            break findCollision;
                        }
                    }
                }
            }
        }

        player.uncertaintyHandler.lastHardCollidingLerpingEntity--;
        if (hasHardCollision) player.uncertaintyHandler.lastHardCollidingLerpingEntity = 0;
    }
}
