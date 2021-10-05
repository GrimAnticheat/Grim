package ac.grim.grimac.checks.impl.prediction;

import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import org.bukkit.GameMode;

public class NoFallB extends PostPredictionCheck {

    public NoFallB(GrimPlayer player) {
        super(player);
    }

    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        // Exemptions
        // Don't check players in spectator
        if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_8) && predictionComplete.getData().gameMode == GameMode.SPECTATOR)
            return;
        // And don't check this long list of ground exemptions
        if (player.exemptOnGround()) return;
        // Don't check if the player was on a ghost block
        if (player.getSetbackTeleportUtil().blockOffsets) return;
        // Viaversion sends wrong ground status... (doesn't matter but is annoying)
        if (predictionComplete.getData().isJustTeleported) return;

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
