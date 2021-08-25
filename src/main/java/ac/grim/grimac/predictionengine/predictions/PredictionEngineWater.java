package ac.grim.grimac.predictionengine.predictions;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.nmsImplementations.Collisions;
import ac.grim.grimac.utils.nmsImplementations.FluidFallingAdjustedMovement;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class PredictionEngineWater extends PredictionEngine {
    boolean isFalling;
    double playerGravity;
    float swimmingSpeed;
    float swimmingFriction;
    double lastY;

    public static void staticVectorEndOfTick(GrimPlayer player, Vector vector, float swimmingFriction, double playerGravity, boolean isFalling) {
        vector.multiply(new Vector(swimmingFriction, 0.8F, swimmingFriction));
        Vector fluidVector = FluidFallingAdjustedMovement.getFluidFallingAdjustedMovement(player, playerGravity, isFalling, vector);
        vector.setX(fluidVector.getX());
        vector.setY(fluidVector.getY());
        vector.setZ(fluidVector.getZ());
    }

    public static Vector getLookAngle(GrimPlayer player) {
        return calculateViewVector(player, player.yRot, player.xRot);
    }

    public static Vector calculateViewVector(GrimPlayer player, float f, float f2) {
        float f3 = f * 0.017453292f;
        float f4 = -f2 * 0.017453292f;
        float f5 = player.trigHandler.cos(f4);
        float f6 = player.trigHandler.sin(f4);
        float f7 = player.trigHandler.cos(f3);
        float f8 = player.trigHandler.sin(f3);
        return new Vector(f6 * f7, -f8, f5 * f7);
    }

    public void guessBestMovement(float swimmingSpeed, GrimPlayer player, boolean isFalling, double playerGravity, float swimmingFriction, double lastY) {
        this.isFalling = isFalling;
        this.playerGravity = playerGravity;
        this.swimmingSpeed = swimmingSpeed;
        this.swimmingFriction = swimmingFriction;
        this.lastY = lastY;
        super.guessBestMovement(swimmingSpeed, player);
    }

    @Override
    public void addJumpsToPossibilities(GrimPlayer player, Set<VectorData> existingVelocities) {
        for (VectorData vector : new HashSet<>(existingVelocities)) {
            existingVelocities.add(vector.returnNewModified(vector.vector.clone().add(new Vector(0, 0.04, 0)), VectorData.VectorType.Jump));

            if (player.slightlyTouchingWater && player.lastOnGround && !player.onGround) {
                Vector withJump = vector.vector.clone();
                super.doJump(player, withJump);
                existingVelocities.add(new VectorData(withJump, vector, VectorData.VectorType.Jump));
            }
        }
    }

    @Override
    public void endOfTick(GrimPlayer player, double playerGravity, float friction) {
        super.endOfTick(player, playerGravity, friction);

        for (VectorData vector : player.getPossibleVelocitiesMinusKnockback()) {
            staticVectorEndOfTick(player, vector.vector, swimmingFriction, playerGravity, isFalling);
        }
    }

    @Override
    public Set<VectorData> fetchPossibleStartTickVectors(GrimPlayer player) {
        // "hacky" climbing where player enters ladder within 0.03 movement
        if (player.lastWasClimbing == 0 && player.isClimbing && (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_14) || !Collisions.isEmpty(player, player.boundingBox.copy().expand(
                player.clientVelocity.getX(), 0, player.clientVelocity.getZ()).expand(0.5, -SimpleCollisionBox.COLLISION_EPSILON, 0.5)))) {
            player.lastWasClimbing = FluidFallingAdjustedMovement.getFluidFallingAdjustedMovement(player, playerGravity, isFalling, player.clientVelocity.clone().setY(0.2D * 0.8F)).getY();
        }

        Set<VectorData> baseVelocities = super.fetchPossibleStartTickVectors(player);

        return transformSwimmingVectors(player, baseVelocities);
    }

    public static Set<VectorData> transformSwimmingVectors(GrimPlayer player, Set<VectorData> base) {
        Set<VectorData> swimmingVelocities = new HashSet<>();

        if (player.isSwimming && player.playerVehicle == null) {
            for (VectorData vector : base) {
                double d = getLookAngle(player).getY();
                double d5 = d < -0.2 ? 0.085 : 0.06;

                // The player can always press jump and activate this
                swimmingVelocities.add(vector.returnNewModified(new Vector(vector.vector.getX(), vector.vector.getY() + ((d - vector.vector.getY()) * d5), vector.vector.getZ()), VectorData.VectorType.SwimmingSpace));

                // This scenario will occur if the player does not press jump and the other conditions are met
                if (d > 0.0 && player.compensatedWorld.getFluidLevelAt(player.lastX, player.lastY + 1.0 - 0.1, player.lastZ) == 0) {
                    swimmingVelocities.add(vector.returnNewModified(vector.vector, VectorData.VectorType.SurfaceSwimming));
                }
            }
            return swimmingVelocities;
        }
        return base;
    }
}
