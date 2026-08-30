package ac.grim.grimac.checks.type;

import ac.grim.grimac.utils.anticheat.update.PredictionComplete;

public interface PostPredictionListener {
    void onPredictionComplete(PredictionComplete predictionComplete);
}
