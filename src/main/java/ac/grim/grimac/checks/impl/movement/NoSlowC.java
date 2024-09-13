package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

@CheckData(name = "NoSlowC", setback = 5, experimental = true)
public class NoSlowC extends Check implements PostPredictionCheck {
    public NoSlowC(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (!predictionComplete.isChecked()) return;

        if (player.isSlowMovement) {
            ClientVersion client = player.getClientVersion();

            // https://bugs.mojang.com/browse/MC-152728
            if (client.isNewerThanOrEquals(ClientVersion.V_1_14_2)) {
                return;
            }

            if (player.isSprinting
                    // you can sneak and swim in 1.13 - 1.14.1
                    && (!player.isSwimming || client.isNewerThan(ClientVersion.V_1_14_1) || client.isOlderThan(ClientVersion.V_1_13))
                    && player.sneakingSpeedMultiplier < 0.8f
            ) {
                if (flagWithSetback()) alert("");
            } else reward();
        }
    }
}
