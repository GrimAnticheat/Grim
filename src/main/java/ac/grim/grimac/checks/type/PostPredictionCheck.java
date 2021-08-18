package ac.grim.grimac.checks.type;

import ac.grim.grimac.utils.anticheat.update.PredictionComplete;

public interface PostPredictionCheck {

    default void onPredictionComplete(final PredictionComplete predictionComplete) {
    }
}
