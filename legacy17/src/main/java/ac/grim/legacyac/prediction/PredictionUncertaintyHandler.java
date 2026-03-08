package ac.grim.legacyac.prediction;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.data.PlayerData;

public final class PredictionUncertaintyHandler {
    private PredictionUncertaintyHandler() {
    }

    public static double reduceOffset(double rawOffset, PlayerData.PredictionContext context, LegacyAntiCheatPlugin plugin) {
        return Math.max(0.0D, rawOffset - resolveBudget(context, plugin));
    }

    public static double reduceOffsetWithLenience(double rawOffset, PlayerData.PredictionContext context,
            LegacyAntiCheatPlugin plugin, double lastHorizontalOffset, double lastVerticalOffset) {
        double budget = resolveBudget(context, plugin);
        double lenience = Math.max(lastHorizontalOffset, lastVerticalOffset);
        budget += lenience;
        return Math.max(0.0D, rawOffset - budget);
    }

    public static double resolveBudget(PlayerData.PredictionContext context, LegacyAntiCheatPlugin plugin) {
        PlayerData data = context.getData();
        double budget = plugin.getConfig().getDouble("prediction.budget.base", 0.025D);

        if (context.isRecentVelocity()) {
            budget += Math.max(0.6D, plugin.getConfig().getDouble("prediction.budget.recent-velocity", 0.6D));
        }
        if (context.isStuckEdge()) {
            budget += plugin.getConfig().getDouble("prediction.budget.stuck-speed", 0.025D);
        }
        if (context.isInLiquid()) {
            budget += plugin.getConfig().getDouble("prediction.budget.liquid", 0.035D);
        }
        if (context.isNearGlitchyBlock()) {
            budget += plugin.getConfig().getDouble("prediction.budget.near-glitchy-block", 0.020D);
        }
        if (context.isNearZeroThreeBoundary()) {
            budget += plugin.getConfig().getDouble("prediction.budget.point-three", 0.020D);
        }
        if (context.isRecentUnevenGround()) {
            budget += Math.max(0.05D, plugin.getConfig().getDouble("prediction.budget.uneven-ground", 0.05D));
        }
        if (context.isRecentSnowLayerGround()) {
            budget += Math.max(0.06D, plugin.getConfig().getDouble("prediction.budget.snow-layer", 0.06D));
        }
        if (context.isNearPartialGround()) {
            budget += Math.max(0.04D, plugin.getConfig().getDouble("prediction.budget.partial-ground", 0.04D));
        }
        if (context.isRecentIceGround()) {
            budget += Math.max(0.07D, plugin.getConfig().getDouble("prediction.budget.ice-ground", 0.07D));
        }
        if (context.isRecentHeadHit()) {
            budget += Math.max(0.09D, plugin.getConfig().getDouble("prediction.budget.head-hit", 0.09D));
        }
        if (context.isRecentRodPull()) {
            budget += plugin.getConfig().getDouble("prediction.budget.rod-pull", 0.050D);
        }
        if (context.isRecentEntityCollision()) {
            budget += plugin.getConfig().getDouble("prediction.budget.entity-hard-collision", 0.030D);
        }
        if (context.isRecentTeleport()) {
            budget += plugin.getConfig().getDouble("prediction.budget.teleport", 0.080D);
        }
        if (context.isRecentHighFall()) {
            budget += Math.max(0.30D, plugin.getConfig().getDouble("prediction.budget.high-fall-recovery", 0.30D));
        }
        if ((context.isRecentUnevenGround() || context.isRecentSnowLayerGround() || context.isRecentIceGround())
                && data.getLastDeltaY() < -0.20D) {
            budget += Math.max(0.08D, plugin.getConfig().getDouble("prediction.budget.uneven-fall", 0.08D));
        }

        if (data.isInventoryOpen() && Math.abs(data.getLastDeltaY()) > 0.05D) {
            budget += Math.max(0.08D, plugin.getConfig().getDouble("prediction.budget.inventory-air", 0.08D));
        }
        if (data.hasRecentUseItemPacket(plugin.getConfig().getLong("checks.NoSlow.use-packet-max-age-ms", 250L))
                && Math.abs(data.getLastDeltaY()) > 0.05D) {
            budget += Math.max(0.08D, plugin.getConfig().getDouble("prediction.budget.use-item-air", 0.08D));
        }

        long timeSinceAttack = System.currentTimeMillis() - data.combat().getLastAttackAt();
        if (timeSinceAttack < 1000L) {
            budget += 0.020D;
        }

        int speedLvl = data.getSpeedLevel();
        if (speedLvl > 0) {
            budget += (0.03D * speedLvl);
            if (speedLvl >= 2) {
                budget += 0.02D;
            }
        } else {
            budget += 0.015D;
        }

        if (data.getLastDeltaXZ() > 0.28D && Math.abs(data.getLastDeltaY()) > 0.20D) {
            budget += Math.max(0.03D, plugin.getConfig().getDouble("prediction.budget.air-sprint-jump", 0.03D));
        }

        return budget;
    }
}
