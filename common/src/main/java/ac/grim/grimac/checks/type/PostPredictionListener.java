package ac.grim.grimac.checks.type;

import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;

public interface PostPredictionListener extends AbstractCheck {
    void onPredictionComplete(PredictionComplete predictionComplete);
}
