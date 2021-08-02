package ac.grim.grimac.predictionengine;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.lists.EvictingList;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
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
    // Is the player within 0.26 of a boat?
    public boolean collidingWithBoat;
    // Is the player within 0.26 blocks of a shulker?
    public boolean collidingWithShulker;
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
    public boolean isSteppingOnBouncyBlock = false;
    public boolean stuckOnEdge = false;
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
    // How many entities are very likely to be colliding with the player's bounding box?
    public EvictingList<Integer> strictCollidingEntities = new EvictingList<>(3);
    // How many entities are within 0.5 blocks of the player's bounding box?
    public EvictingList<Integer> collidingEntities = new EvictingList<>(3);
    public EvictingList<Double> pistonPushing = new EvictingList<>(20);
    public EvictingList<Boolean> flyingStatusSwitchHack = new EvictingList<>(3);
    public EvictingList<Boolean> legacyUnderwaterFlyingHack = new EvictingList<>(10);
    public EvictingList<Boolean> stuckMultiplierZeroPointZeroThree = new EvictingList<>(5);
    public EvictingList<Boolean> boatCollision = new EvictingList<>(3);
    public int lastTeleportTicks = 0;
    public int lastFlyingTicks = 0;
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
        collidingWithBoat = false;
        collidingWithShulker = false;
        isStepMovement = false;
        stuckOnEdge = false;
        slimePistonBounces = new HashSet<>();
    }

    // 0.04 is safe for speed 10, 0.03 is unsafe
    // 0.0016 is safe for speed 1, 0.09 is unsafe
    //
    // Taking these approximate values gives us this, the same 0.03 value for each speed
    // Don't give bonus for sprinting because sprinting against walls isn't possible
    public double getZeroPointZeroThreeThreshold() {
        return 0.096 * (player.speed / (player.isSprinting ? 1.3d : 1)) - 0.008;
    }

    public boolean countsAsZeroPointZeroThree(VectorData predicted) {
        // First tick movement should always be considered zero point zero three
        // Shifting movement is somewhat buggy because 0.03
        if (player.isFirstTick || stuckOnEdge || wasAffectedByStuckSpeed())
            return true;

        // Explicitly is 0.03 movement
        if (predicted.hasVectorType(VectorData.VectorType.ZeroPointZeroThree))
            return true;

        // Movement is too low to determine whether this is zero point zero three
        if (player.couldSkipTick && player.actualMovement.lengthSquared() < 0.01)
            return true;

        if ((lastFlyingTicks > -3) && Math.abs(predicted.vector.getY()) < 0.2 && predicted.vector.getY() != 0 && player.actualMovement.lengthSquared() < 0.2)
            return true;

        return isSteppingOnIce && lastTickWasNearGroundZeroPointZeroThree && player.actualMovement.clone().setY(0).lengthSquared() < 0.01;
    }

    public boolean wasAffectedByStuckSpeed() {
        return !stuckMultiplierZeroPointZeroThree.isEmpty() && Collections.max(stuckMultiplierZeroPointZeroThree);
    }

    public double getOffsetHorizontal(VectorData data) {
        double pointThree = stuckOnEdge || data.hasVectorType(VectorData.VectorType.ZeroPointZeroThree) ? 0.06 : lastMovementWasZeroPointZeroThree ? 0.06 : lastLastMovementWasZeroPointZeroThree ? 0.03 : 0;

        // 0.03 plus being able to maintain velocity even when shifting is brutal
        if (stuckOnEdge && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_14))
            pointThree = Math.max(pointThree, player.speed / 3);

        if (data.hasVectorType(VectorData.VectorType.ZeroPointZeroThree) && player.uncertaintyHandler.isSteppingOnBouncyBlock)
            pointThree = Math.max(pointThree, 0.1);

        if (lastTeleportTicks > -3)
            pointThree = Math.max(pointThree, 0.1);

        if (wasAffectedByStuckSpeed())
            pointThree = Math.max(pointThree, 0.08);

        if (player.uncertaintyHandler.scaffoldingOnEdge) {
            pointThree = Math.max(pointThree, player.speed * 1.5);
        }

        if (Collections.max(boatCollision)) {
            pointThree = Math.max(pointThree, 1);
        }

        return pointThree;
    }

    public double getVerticalOffset(VectorData data) {
        // Not worth my time to fix this because checking flying generally sucks - if player was flying in last 2 ticks
        if ((lastFlyingTicks > -3) && Math.abs(data.vector.getY()) < (4.5 * player.flySpeed - 0.25))
            return 0.225;

        if (Collections.max(boatCollision))
            return 1;

        if (data.hasVectorType(VectorData.VectorType.ZeroPointZeroThree) && player.uncertaintyHandler.isSteppingOnBouncyBlock)
            return 0.1;

        // I don't understand this either.  0.03 in lava just really sucks.
        if (wasLastGravityUncertain && player.wasTouchingLava)
            return 0.2;

        if (wasLastGravityUncertain)
            return 0.03;

        if (!controlsVerticalMovement() || data.hasVectorType(VectorData.VectorType.Jump))
            return 0;

        return data.hasVectorType(VectorData.VectorType.ZeroPointZeroThree) ? 0.06 : lastMovementWasZeroPointZeroThree ? 0.06 : lastLastMovementWasZeroPointZeroThree ? 0.03 : 0;
    }

    public boolean controlsVerticalMovement() {
        return player.wasTouchingWater || player.wasTouchingLava || isSteppingOnBouncyBlock || lastFlyingTicks > -3 || player.isGliding;
    }

    public boolean canSkipTick(List<VectorData> possibleVelocities) {
        // 0.03 is very bad with stuck speed multipliers
        if (player.inVehicle) {
            return false;
        } else if (player.uncertaintyHandler.wasAffectedByStuckSpeed()) {
            player.uncertaintyHandler.gravityUncertainty = -0.08;
            return true;
        } else if (player.uncertaintyHandler.isSteppingOnBouncyBlock && Math.abs(player.clientVelocity.getY()) < 0.2) {
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

    @Override
    public String toString() {
        return "UncertaintyHandler{" +
                "pistonX=" + pistonX +
                ", pistonY=" + pistonY +
                ", pistonZ=" + pistonZ +
                ", collidingWithBoat=" + collidingWithBoat +
                ", collidingWithShulker=" + collidingWithShulker +
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
