package ac.grim.grimac.checks.impl.prediction;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;

// Fucking FastMath/BetterFPS playing with our trig tables requiring us to not ban players for 1e-4 offsets
// We can only really set them back and kick them :(
// As much as I want to ban FastMath users for cheating, the current consensus is that it doesn't matter.
//
// Buffer this heavily because the cheats that change movement less than 0.0001/tick don't matter much
@CheckData(name = "Prediction (Minor)", buffer = 50)
public class SmallOffsetHandler extends PostPredictionCheck {
    public SmallOffsetHandler(GrimPlayer player) {
        super(player);
    }

    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        double offset = predictionComplete.getOffset();

        if (offset > 0.0001) {
            decreaseBuffer(1);

            if (getBuffer() == 0) {
                player.getSetbackTeleportUtil().executeSetback();
            }
        } else {
            increaseBuffer(0.25);
        }

        if (getBuffer() > 50) {
            setBuffer(50);
        }
    }
}
