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

    /**
     * Reduce offset with Grim's offset lenience from previous tick.
     * This is the key mechanism Grim uses to prevent cascading false positives:
     * when a tick flags, the offset is carried into the next tick as extra tolerance.
     */
    public static double reduceOffsetWithLenience(double rawOffset, PlayerData.PredictionContext context,
            LegacyAntiCheatPlugin plugin, double lastHorizontalOffset, double lastVerticalOffset) {
        double budget = resolveBudget(context, plugin);
        // Add previous tick's offset as extra tolerance (Grim OffsetHandler pattern)
        double lenience = Math.max(lastHorizontalOffset, lastVerticalOffset);
        budget += lenience;
        return Math.max(0.0D, rawOffset - budget);
    }

    public static double resolveBudget(PlayerData.PredictionContext context, LegacyAntiCheatPlugin plugin) {
        // Base budget - increased for 1.7.10 float jitter (vanilla is 0.005, but we need more for GLAC)
        double budget = plugin.getConfig().getDouble("prediction.budget.base", 0.025D);

        if (context.isRecentVelocity()) {
            // Velocity budget - increased significantly for KB handling
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
            budget += Math.max(0.04D, plugin.getConfig().getDouble("prediction.budget.uneven-ground", 0.04D));
        }
        if (context.isRecentSnowLayerGround()) {
            budget += Math.max(0.05D, plugin.getConfig().getDouble("prediction.budget.snow-layer", 0.05D));
        }
        if (context.isNearPartialGround()) {
            budget += Math.max(0.03D, plugin.getConfig().getDouble("prediction.budget.partial-ground", 0.03D));
        }
        if (context.isRecentRodPull()) {
            budget += plugin.getConfig().getDouble("prediction.budget.rod-pull", 0.050D);
        }
        if (context.isRecentEntityCollision()) {
            budget += plugin.getConfig().getDouble("prediction.budget.entity-hard-collision", 0.030D);
        }
        if (context.isRecentTeleport()) {
            // Teleports in 1.7.10 often have 1-tick alignment delay
            budget += plugin.getConfig().getDouble("prediction.budget.teleport", 0.080D);
        }
        if (context.isRecentHighFall()) {
            budget += Math.max(0.30D, plugin.getConfig().getDouble("prediction.budget.high-fall-recovery", 0.30D));
        }
        if ((context.isRecentUnevenGround() || context.isRecentSnowLayerGround()) && context.getData().getLastDeltaY() < -0.20D) {
            budget += Math.max(0.08D, plugin.getConfig().getDouble("prediction.budget.uneven-fall", 0.08D));
        }

        // Combat awareness: rapid yaw changes and hits increase uncertainty
        long timeSinceAttack = System.currentTimeMillis() - context.getData().combat().getLastAttackAt();
        if (timeSinceAttack < 1000L) {
            budget += 0.020D; // Combat jitter allowance
        }

        // Dynamic Speed potion buffer scaling - increased multiplier
        int speedLvl = context.getData().getSpeedLevel();
        if (speedLvl > 0) {
            budget += (0.015D * speedLvl); // Scalable buffer based on amp
        } else {
            budget += 0.015D; // Small base buffer (non-speed)
        }

        return budget;
    }
}

