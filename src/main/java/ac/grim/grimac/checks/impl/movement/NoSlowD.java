package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

@CheckData(name = "NoSlowD", setback = 5, experimental = true)
public class NoSlowD extends Check implements PostPredictionCheck {
    public NoSlowD(GrimPlayer player) {
        super(player);
    }

    private boolean flaggedLastTick = false;

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (!predictionComplete.isChecked()) return;

        if (player.packetStateData.isSlowedByUsingItem()) {
            // https://bugs.mojang.com/browse/MC-152728
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14_2)) {
                return;
            }

            if (player.isSprinting) {
                if (flaggedLastTick && flagWithSetback()) alert("");
                flaggedLastTick = true;
            } else {
                reward();
                flaggedLastTick = false;
            }
        }
    }
}
