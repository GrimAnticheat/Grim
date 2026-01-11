package ac.grim.grimac.predictionengine.predictions.rideable;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.predictions.PredictionEngine;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.Vector3dm;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class PredictionEngineNautilusWater extends PredictionEngine {
    private final Vector3dm movementVector;
    private final double multiplier;

    @Override
    public void endOfTick(GrimPlayer player, double delta) {
        super.endOfTick(player, delta);

        for (VectorData vector : player.getPossibleVelocitiesMinusKnockback()) {
            vector.vector.setX(vector.vector.getX() * multiplier);
            vector.vector.setY(vector.vector.getY() * multiplier);
            vector.vector.setZ(vector.vector.getZ() * multiplier);
        }
    }

    @Override
    public void addJumpsToPossibilities(GrimPlayer player, Set<VectorData> existingVelocities) {
        PredictionEngineRideableUtils.handleJumps(player, existingVelocities);
    }

    @Override
    public List<VectorData> applyInputsToVelocityPossibilities(GrimPlayer player, Set<VectorData> possibleVectors, float speed) {
        return PredictionEngineRideableUtils.applyInputsToVelocityPossibilities(this, movementVector, player, possibleVectors, speed);
    }

    @Override
    public Vector3dm getMovementResultFromInput(GrimPlayer player, Vector3dm inputVector, float flyingSpeed, float yRot) {
        float yRotRadians = GrimMath.radians(yRot);
        float sin = player.trigHandler.sin(yRotRadians);
        float cos = player.trigHandler.cos(yRotRadians);

        double xResult = inputVector.getX() * cos - inputVector.getZ() * sin;
        double zResult = inputVector.getZ() * cos + inputVector.getX() * sin;

        return new Vector3dm(xResult * flyingSpeed, inputVector.getY() * flyingSpeed, zResult * flyingSpeed);
    }

}
