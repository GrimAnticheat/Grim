package ac.grim.grimac.checks.impl.prediction;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;

// This is for large offsets for stuff such as jesus, large speed, and almost all cheats
// SlowMath and other stupid trig tables will not flag the check, except for that one trig
// table that literally does Math.rand().  We don't support that trig table.
@CheckData(name = "Prediction (Major)", buffer = 0)
public class LargeOffsetHandler extends PostPredictionCheck {
    public LargeOffsetHandler(GrimPlayer player) {
        super(player);
    }

    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        double offset = predictionComplete.getOffset();

        if (offset > 0.01) {
            player.setbackTeleportUtil.executeSetback();
        }
    }
}
