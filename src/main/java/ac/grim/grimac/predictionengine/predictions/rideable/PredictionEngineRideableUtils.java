package ac.grim.grimac.predictionengine.predictions.rideable;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.predictions.PredictionEngine;
import ac.grim.grimac.predictionengine.predictions.PredictionEngineNormal;
import ac.grim.grimac.utils.data.VectorData;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PredictionEngineRideableUtils {
    public static Set<VectorData> fetchPossibleStartTickVectors(GrimPlayer player, Set<VectorData> vectorData) {
        for (VectorData data : vectorData) {
            data.vector.multiply(0.98);
        }

        return vectorData;
    }

    public static List<VectorData> applyInputsToVelocityPossibilities(Vector movementVector, GrimPlayer player, Set<VectorData> possibleVectors, float speed) {
        List<VectorData> returnVectors = new ArrayList<>();

        for (VectorData possibleLastTickOutput : possibleVectors) {
            VectorData result = new VectorData(possibleLastTickOutput.vector.clone().add(new PredictionEngine().getMovementResultFromInput(player, movementVector, speed, player.xRot)), possibleLastTickOutput, VectorData.VectorType.InputResult);
            result = result.setVector(result.vector.clone().multiply(player.stuckSpeedMultiplier), VectorData.VectorType.StuckMultiplier);
            result = result.setVector(new PredictionEngineNormal().handleOnClimbable(result.vector.clone(), player), VectorData.VectorType.Climbable);
            returnVectors.add(result);
        }

        return returnVectors;
    }
}
