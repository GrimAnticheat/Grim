package ac.grim.grimac.checks.impl.prediction;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

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
        } else {
            increaseBuffer(0.125);
        }

        if (getBuffer() == 0) {
            player.teleportUtil.blockMovementsUntilResync(player.playerWorld, new Vector3d(player.lastX, player.lastY, player.lastZ), player.packetStateData.packetPlayerXRot, player.packetStateData.packetPlayerYRot, player.clientVelocity, player.vehicle, player.lastTransactionReceived);
            Bukkit.broadcastMessage(ChatColor.RED + "Small buffer has run out!  Setting back");
        }

        if (getBuffer() > 5) {
            setBuffer(5);
        }
    }
}
