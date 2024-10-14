package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;

public class NoSlowMitigation extends Check implements PostPredictionCheck {
    private double offsetToFlag;
    private double bestOffset = 1;
    private boolean flaggedLastTick;

    public NoSlowMitigation(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (!predictionComplete.isChecked()) return;

        if (wouldFlag()) {
            if (flaggedLastTick) {
                player.resetBukkitItemUsage();
            }
            flaggedLastTick = true;
        } else flaggedLastTick = false;

        bestOffset = 1;
    }

    public void handlePredictionAnalysis(double offset) {
        bestOffset = Math.min(bestOffset, offset);
    }

    public boolean wouldFlag() {
        return GrimAPI.INSTANCE.getConfigManager().isMitigateNoSlow()
                && !player.packetStateData.isSlowedByUsingItem()
                && bestOffset > offsetToFlag
                && player.isUsingBukkitItem();
    }

    @Override
    public void onReload(ConfigManager config) {
        offsetToFlag = config.getDoubleElse("mitigate-noslow-threshold", 0.001);
    }
}
