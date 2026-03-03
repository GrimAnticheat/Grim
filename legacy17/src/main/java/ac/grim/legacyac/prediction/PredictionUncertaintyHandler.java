package ac.grim.legacyac.prediction;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.data.PlayerData;

public final class PredictionUncertaintyHandler {
    private PredictionUncertaintyHandler() {
    }

    public static double reduceOffset(double rawOffset, PlayerData.PredictionContext context, LegacyAntiCheatPlugin plugin) {
        double reduced = Math.max(0.0D, rawOffset - resolveBudget(context, plugin));
        return reduced;
    }

    public static double resolveBudget(PlayerData.PredictionContext context, LegacyAntiCheatPlugin plugin) {
        double budget = plugin.getConfig().getDouble("prediction.budget.base", 0.012D);

        if (context.isRecentVelocity()) {
            budget += plugin.getConfig().getDouble("prediction.budget.recent-velocity", 0.022D);
        }
        if (context.isStuckEdge()) {
            budget += plugin.getConfig().getDouble("prediction.budget.stuck-speed", 0.018D);
        }
        if (context.isInLiquid()) {
            budget += plugin.getConfig().getDouble("prediction.budget.liquid", 0.028D);
        }
        if (context.isNearGlitchyBlock()) {
            budget += plugin.getConfig().getDouble("prediction.budget.near-glitchy-block", 0.016D);
        }
        if (context.isNearZeroThreeBoundary()) {
            budget += plugin.getConfig().getDouble("prediction.budget.point-three", 0.018D);
        }
        if (context.isRecentRodPull()) {
            budget += plugin.getConfig().getDouble("prediction.budget.rod-pull", 0.040D);
        }
        if (context.isRecentEntityCollision()) {
            budget += plugin.getConfig().getDouble("prediction.budget.entity-hard-collision", 0.020D);
        }
        if (context.isRecentTeleport()) {
            budget += plugin.getConfig().getDouble("prediction.budget.teleport", 0.020D);
        }
        if (context.isRecentHighFall()) {
            budget += plugin.getConfig().getDouble("prediction.budget.high-fall-recovery", 0.050D);
        }

        return budget;
    }
}
