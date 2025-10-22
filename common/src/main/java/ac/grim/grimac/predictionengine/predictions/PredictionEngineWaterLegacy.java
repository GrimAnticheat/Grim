package ac.grim.grimac.predictionengine.predictions;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.Vector3dm;

import java.util.HashSet;
import java.util.Set;

public class PredictionEngineWaterLegacy extends PredictionEngine {
    private float swimmingSpeed;
    private float swimmingFriction;

    public void guessBestMovement(float swimmingSpeed, GrimPlayer player, float swimmingFriction) {
        this.swimmingSpeed = swimmingSpeed;
        this.swimmingFriction = swimmingFriction;
        super.guessBestMovement(swimmingSpeed, player);
    }

    // This is just the vanilla equation for legacy water movement
    @Override
    public Vector3dm getMovementResultFromInput(GrimPlayer player, double x, double y, double z, float f, float f2) {
        float lengthSquared = (float) GrimMath.lengthSquared(x, y, z);

        if (lengthSquared >= 1.0E-4F) {
            lengthSquared = (float) Math.sqrt(lengthSquared);

            if (lengthSquared < 1.0F) {
                lengthSquared = 1.0F;
            }

            lengthSquared = swimmingSpeed / lengthSquared;
            x *= lengthSquared;
            y *= lengthSquared;
            z *= lengthSquared;
            float yawRadians = GrimMath.radians(player.yaw);
            float sinResult = player.trigHandler.sin(yawRadians);
            float cosResult = player.trigHandler.cos(yawRadians);

            return new Vector3dm(x * cosResult - z * sinResult,
                    y, z * cosResult + x * sinResult);
        }

        return new Vector3dm();
    }


    @Override
    public void addJumpsToPossibilities(GrimPlayer player, Set<VectorData> existingVelocities) {
        final VectorData[] vectorsToProcess = existingVelocities.toArray(new VectorData[0]);
        for (VectorData vector : vectorsToProcess) {
            existingVelocities.add(new VectorData(vector.vectorX, vector.vectorY + 0.04f, vector.vectorZ, vector, VectorData.VectorType.Jump));

            if (player.skippedTickInActualMovement) {
                existingVelocities.add(new VectorData(vector.vectorX,vector.vectorY + 0.02f, vector.vectorZ, vector, VectorData.VectorType.Jump));
            }
        }
    }

    @Override
    public void endOfTick(GrimPlayer player, double playerGravity) {
        super.endOfTick(player, playerGravity);

        for (VectorData vector : player.getPossibleVelocitiesMinusKnockback()) {
            vector.vectorX *= swimmingFriction;
            vector.vectorY *= 0.8F;
            vector.vectorZ *= swimmingFriction;

            // Gravity
            vector.vectorY -= 0.02D;
        }
    }
}
