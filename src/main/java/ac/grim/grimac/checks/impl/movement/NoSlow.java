package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.data.AlmostBoolean;

@CheckData(name = "NoSlow (Prediction)", configName = "NoSlow", buffer = 10, maxBuffer = 15)
public class NoSlow extends PostPredictionCheck {
    public NoSlow(GrimPlayer player) {
        super(player);
    }

    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        // If the player was using an item for certain, and their predicted velocity had a flipped item
        if (predictionComplete.getData().isUsingItem == AlmostBoolean.TRUE) {
            if (player.predictedVelocity.isFlipItem()) { // prediction had using item = false
                increaseViolations();
                alert("", "NoSlow", formatViolations());
            } else { // prediction had using item = true when using item
                reward();
            }
        }
    }
}
