package ac.grim.grimac.checks.impl.prediction;

import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;

public class NoFallB extends PostPredictionCheck {

    public NoFallB(GrimPlayer player) {
        super(player);
    }

    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        // Exemptions
        if (player.exemptOnGround()) return;

        boolean invalid = player.clientClaimsLastOnGround != player.onGround;

        if (invalid) {
            increaseViolations();
            alert("claimed " + player.clientClaimsLastOnGround, "GroundSpoof (Prediction)", formatViolations());

            if (player.onGround && getViolations() > getSetbackVL()) {
                player.checkManager.getNoFall().playerUsingNoGround = true;
            }
        }
    }
}
