package ac.grim.grimac.checks.impl.prediction;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

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
            player.teleportUtil.blockMovementsUntilResync(player.playerWorld, new Vector3d(player.lastX, player.lastY, player.lastZ), player.xRot, player.yRot, player.clientVelocity, player.vehicle, player.lastTransactionReceived);
            Bukkit.broadcastMessage(ChatColor.RED + "Large offset detected!  Setting back. Offset: " + offset);
        }
    }
}
