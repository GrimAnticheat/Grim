package ac.grim.grimac.predictionengine.predictions.rideable;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.predictions.PredictionEngineNormal;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.math.Vector3dm;

import java.util.List;
import java.util.Set;

public class HappyGhastPredictionEngine extends PredictionEngineNormal {

    final Vector3dm movementVector;
    final double multiplier;

    public HappyGhastPredictionEngine(Vector3dm movementVector, double multiplier) {
        this.movementVector = movementVector;
        this.multiplier = multiplier;
    }

    @Override
    public void endOfTick(GrimPlayer player, double delta) {
        for (VectorData vector : player.getPossibleVelocitiesMinusKnockback()) {
            vector.vector.setX(vector.vector.getX() * multiplier);
            vector.vector.setY(vector.vector.getY() * multiplier);
            vector.vector.setZ(vector.vector.getZ() * multiplier);
        }
    }

    @Override
    public void addJumpsToPossibilities(GrimPlayer player, Set<VectorData> existingVelocities) {
        // no-op
    }

    @Override
    public List<VectorData> applyInputsToVelocityPossibilities(GrimPlayer player, Set<VectorData> possibleVectors, float speed) {
        return PredictionEngineRideableUtils.applyInputsToVelocityPossibilities(this, movementVector, player, possibleVectors, speed);
    }

    @Override
    public Vector3dm getMovementResultFromInput(GrimPlayer player, Vector3dm vec3, float flyingSpeed, float yRot) {
        double length = vec3.lengthSquared();
        if (length < 1.0E-7) {
            return new Vector3dm();
        } else {
            Vector3dm normalized = (length > 1.0 ? vec3.clone().normalize() : vec3.clone()).multiply(flyingSpeed);
            float sin = player.trigHandler.sin(yRot * (float) (Math.PI / 180.0));
            float cos = player.trigHandler.cos(yRot * (float) (Math.PI / 180.0));
            return new Vector3dm(normalized.getX() * cos - normalized.getZ() * sin, normalized.getY(), normalized.getZ() * cos + normalized.getX() * sin);
        }
    }

}
