package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.data.AlmostBoolean;

@CheckData(name = "NoSlow (Prediction)", configName = "NoSlow", buffer = 10, maxBuffer = 15)
public class NoSlow extends PostPredictionCheck {
    double offsetToFlag;
    double bestOffset = 1;

    public NoSlow(GrimPlayer player) {
        super(player);
    }

    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        // If the player was using an item for certain, and their predicted velocity had a flipped item
        if (predictionComplete.getData().isUsingItem == AlmostBoolean.TRUE) {
            if (bestOffset > offsetToFlag) {
                increaseViolations();
                alert("", "NoSlow", formatViolations());
            } else {
                reward();
            }
        }
        bestOffset = 1;
    }

    public void handlePredictionAnalysis(double offset) {
        bestOffset = Math.min(bestOffset, offset);
    }

    @Override
    public void reload() {
        super.reload();
        offsetToFlag = getConfig().getDouble("NoSlow.threshold", 0.00001);
    }
}
