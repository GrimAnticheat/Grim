package ac.grim.grimac.checks.type;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;

public class PostPredictionCheck extends PacketCheck {

    public PostPredictionCheck(GrimPlayer player) {
        super(player);
    }

    public void onPredictionComplete(final PredictionComplete predictionComplete) {
    }
}
