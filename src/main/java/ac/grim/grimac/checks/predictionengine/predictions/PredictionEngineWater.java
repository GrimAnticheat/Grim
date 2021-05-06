package ac.grim.grimac.checks.predictionengine.predictions;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.chunks.ChunkCache;
import ac.grim.grimac.utils.data.VectorData;
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
    public void addJumpsToPossibilities(GrimPlayer grimPlayer, Set<VectorData> existingVelocities) {
        for (VectorData vector : new HashSet<>(existingVelocities)) {
            existingVelocities.add(new VectorData(vector.vector.clone().add(new Vector(0, 0.04, 0)), vector.vectorType));
            Vector withJump = vector.vector.clone();
            super.doJump(grimPlayer, withJump);
            existingVelocities.add(new VectorData(withJump, vector.vectorType));
        }
    }

    @Override
    public Set<VectorData> fetchPossibleInputs(GrimPlayer grimPlayer) {
        Set<VectorData> baseVelocities = super.fetchPossibleInputs(grimPlayer);
        Set<VectorData> swimmingVelocities = new HashSet<>();

        if (grimPlayer.isSwimming && grimPlayer.playerVehicle == null) {
            for (VectorData vector : baseVelocities) {
                double d = MovementVectorsCalc.getLookAngle(grimPlayer).getY();
                double d5 = d < -0.2 ? 0.085 : 0.06;

                // The player can always press jump and activate this
                swimmingVelocities.add(new VectorData(vector.vector.getX(), vector.vector.getY() + ((d - vector.vector.getY()) * d5), vector.vector.getZ(), vector.vectorType));

                // This scenario will occur if the player does not press jump and the other conditions are met
                if (d > 0.0 && ChunkCache.getFluidLevelAt(grimPlayer.lastX, grimPlayer.lastY + 1.0 - 0.1, grimPlayer.lastZ) == 0) {
                    swimmingVelocities.add(new VectorData(vector.vector, vector.vectorType));
                }
            }

            return swimmingVelocities;
        }

        return baseVelocities;
    }

    @Override
    public void endOfTick(GrimPlayer grimPlayer, double playerGravity, float friction) {
        for (VectorData vector : grimPlayer.getPossibleVelocitiesMinusKnockback()) {
            staticVectorEndOfTick(grimPlayer, vector.vector, swimmingFriction, playerGravity, isFalling);
        }

        super.endOfTick(grimPlayer, playerGravity, friction);
    }
}
