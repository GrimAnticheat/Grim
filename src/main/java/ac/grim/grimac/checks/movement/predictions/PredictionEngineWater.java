package ac.grim.grimac.checks.movement.predictions;

import ac.grim.grimac.GrimPlayer;
import ac.grim.grimac.utils.chunks.ChunkCache;
import ac.grim.grimac.utils.math.MovementVectorsCalc;
import ac.grim.grimac.utils.nmsImplementations.FluidFallingAdjustedMovement;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class PredictionEngineWater extends PredictionEngine {
    boolean isFalling;
    double playerGravity;
    float swimmingSpeed;
    float swimmingFriction;
    double lastY;

    public static void staticVectorEndOfTick(GrimPlayer grimPlayer, Vector vector, float swimmingFriction, double playerGravity, boolean isFalling) {
        vector.multiply(new Vector(swimmingFriction, 0.8F, swimmingFriction));
        Vector fluidVector = FluidFallingAdjustedMovement.getFluidFallingAdjustedMovement(grimPlayer, playerGravity, isFalling, vector);
        vector.setX(fluidVector.getX());
        vector.setY(fluidVector.getY());
        vector.setZ(fluidVector.getZ());
    }

    public void guessBestMovement(float swimmingSpeed, GrimPlayer grimPlayer, boolean isFalling, double playerGravity, float swimmingFriction, double lastY) {
        this.isFalling = isFalling;
        this.playerGravity = playerGravity;
        this.swimmingSpeed = swimmingFriction;
        this.swimmingFriction = swimmingFriction;
        this.lastY = lastY;
        super.guessBestMovement(swimmingSpeed, grimPlayer);
    }

    @Override
    public void addJumpsToPossibilities(GrimPlayer grimPlayer, Set<Vector> existingVelocities) {
        for (Vector vector : new HashSet<>(existingVelocities)) {
            existingVelocities.add(vector.clone().add(new Vector(0, 0.04, 0)));
            Vector withJump = vector.clone();
            super.doJump(grimPlayer, withJump);
            existingVelocities.add(withJump);
        }
    }

    @Override
    public Set<Vector> fetchPossibleInputs(GrimPlayer grimPlayer) {
        Set<Vector> baseVelocities = super.fetchPossibleInputs(grimPlayer);
        Set<Vector> swimmingVelocities = new HashSet<>();

        if (grimPlayer.isSwimming && grimPlayer.playerVehicle == null) {
            for (Vector vector : baseVelocities) {
                double d = MovementVectorsCalc.getLookAngle(grimPlayer).y;
                double d5 = d < -0.2 ? 0.085 : 0.06;

                // The player can always press jump and activate this
                swimmingVelocities.add(new Vector(vector.getX(), vector.getY() + ((d - vector.getY()) * d5), vector.getZ()));

                // This scenario will occur if the player does not press jump and the other conditions are met
                if (d > 0.0 && ChunkCache.getBlockDataAt(grimPlayer.lastX, grimPlayer.lastY + 1.0 - 0.1, grimPlayer.lastZ).getFluid().isEmpty()) {
                    swimmingVelocities.add(vector);
                }
            }

            return swimmingVelocities;
        }

        return baseVelocities;
    }

    @Override
    public void endOfTick(GrimPlayer grimPlayer, double playerGravity, float friction) {
        for (Vector vector : grimPlayer.getPossibleVelocitiesMinusKnockback()) {
            staticVectorEndOfTick(grimPlayer, vector, swimmingFriction, playerGravity, isFalling);
        }

        super.endOfTick(grimPlayer, playerGravity, friction);
    }
}
