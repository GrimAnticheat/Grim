package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;

@CheckData(name = "NoSlow (Prediction)", configName = "NoSlow", setback = 5, dontAlertUntil = 25, alertInterval = 25)
public class NoSlow extends PostPredictionCheck {
    double offsetToFlag;
    double bestOffset = 1;

    public NoSlow(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        // If the player was using an item for certain, and their predicted velocity had a flipped item
        if (player.packetStateData.slowedByUsingItem) {
            if (bestOffset > offsetToFlag) {
                flagWithSetback();
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
