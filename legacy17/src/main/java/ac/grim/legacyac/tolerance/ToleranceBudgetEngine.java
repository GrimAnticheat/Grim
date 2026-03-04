package ac.grim.legacyac.tolerance;

import ac.grim.legacyac.data.state.CompensationState;
import ac.grim.legacyac.data.state.EnvironmentState;
import ac.grim.legacyac.data.state.NetworkState;
import java.util.Locale;

/**
 * Unified Tolerance Budget Engine (FR-3).
 *
 * <p>
 * Combines RTT, Jitter, TPS, and event context to produce a single
 * tolerance budget snapshot that all checks consume. Replaces the scattered
 * per-check threshold logic.
 * </p>
 *
 * <p>
 * A budget snapshot is generated once per frame and injected into the
 * check context. Checks only read the budget — they never recompute it.
 * </p>
 */
public final class ToleranceBudgetEngine {

    private ToleranceBudgetEngine() {
    }

    /**
     * Compute a fresh budget snapshot for the current frame.
     *
     * @param network        network state (RTT, jitter)
     * @param compensation   compensation state (pending changes, velocity)
     * @param environment    environment state (liquid, glitchy, velocity,
     *                       teleport…)
     * @param currentTps     server TPS (from CheckManager)
     * @param configProvider configuration values provider
     * @return immutable budget snapshot
     */
    public static BudgetSnapshot compute(NetworkState network,
            CompensationState compensation,
            EnvironmentState environment,
            double currentTps,
            ConfigProvider configProvider) {

        double rttMs = network.getRttMillis();
        double jitterMs = network.getJitterMillis();

        // ── Core latency factor ────────────────────────────────────────
        // Normalised: 0.0 at 0 ping, ~1.0 at 200ms ping
        double latencyFactor = Math.min(1.0D, rttMs / 200.0D);

        // Jitter penalty: high jitter adds more tolerance
        double jitterFactor = Math.min(0.5D, jitterMs / 100.0D);

        // TPS factor: below 20 TPS grants more tolerance
        double tpsFactor = Math.max(0.0D, (20.0D - currentTps) / 20.0D);

        // ── Scenario addons ────────────────────────────────────────────
        double scenarioAddon = 0.0D;
        if (environment.isRecentVelocity())
            scenarioAddon += configProvider.getRecentVelocityBudget();
        if (environment.isInLiquid())
            scenarioAddon += configProvider.getLiquidBudget();
        if (environment.isNearGlitchyBlock())
            scenarioAddon += configProvider.getGlitchyBlockBudget();
        if (environment.isNearZeroThreeBoundary())
            scenarioAddon += configProvider.getPointThreeBudget();
        if (environment.isStuckEdge())
            scenarioAddon += configProvider.getStuckSpeedBudget();
        if (environment.isRecentRodPull())
            scenarioAddon += configProvider.getRodPullBudget();
        if (environment.isRecentEntityCollision())
            scenarioAddon += configProvider.getEntityCollisionBudget();
        if (environment.isRecentHighFall())
            scenarioAddon += configProvider.getHighFallBudget();
        if (environment.isRecentTeleport())
            scenarioAddon += configProvider.getTeleportBudget();

        // ── Pending state margin ───────────────────────────────────────
        int pendingChanges = compensation.getPendingWorldChangesCount();
        double pendingMargin = pendingChanges > 0 ? configProvider.getPendingStateMargin() : 0.0D;

        // ── Final budgets ──────────────────────────────────────────────
        double baseBudget = configProvider.getBaseBudget();

        double movementAllowance = baseBudget
                + (latencyFactor * configProvider.getSpeedToleranceMultiplier() * 0.1D)
                + (jitterFactor * 0.06D)
                + (tpsFactor * 0.04D)
                + scenarioAddon
                + pendingMargin;

        double combatReachMargin = configProvider.getReachExtraDistance()
                + (latencyFactor * 0.1D)
                + (jitterFactor * 0.05D)
                + (tpsFactor * 0.03D);

        double velocityResponseSlack = configProvider.getSmallMargin()
                + (latencyFactor * 0.04D)
                + (jitterFactor * 0.03D)
                + (tpsFactor * 0.02D);

        return new BudgetSnapshot(
                movementAllowance,
                combatReachMargin,
                velocityResponseSlack,
                rttMs,
                jitterMs,
                currentTps,
                latencyFactor,
                jitterFactor,
                tpsFactor,
                scenarioAddon,
                pendingMargin,
                environment.getScenarioTag());
    }

    // ═══════════════════════════════════════════════════════════════════
    // Budget Snapshot (immutable, one per frame per player)
    // ═══════════════════════════════════════════════════════════════════

    public static final class BudgetSnapshot {
        private final double movementAllowance;
        private final double combatReachMargin;
        private final double velocityResponseSlack;
        private final double rttMs;
        private final double jitterMs;
        private final double tps;
        private final double latencyFactor;
        private final double jitterFactor;
        private final double tpsFactor;
        private final double scenarioAddon;
        private final double pendingMargin;
        private final String scenarioTag;

        BudgetSnapshot(double movementAllowance, double combatReachMargin, double velocityResponseSlack,
                double rttMs, double jitterMs, double tps,
                double latencyFactor, double jitterFactor, double tpsFactor,
                double scenarioAddon, double pendingMargin, String scenarioTag) {
            this.movementAllowance = movementAllowance;
            this.combatReachMargin = combatReachMargin;
            this.velocityResponseSlack = velocityResponseSlack;
            this.rttMs = rttMs;
            this.jitterMs = jitterMs;
            this.tps = tps;
            this.latencyFactor = latencyFactor;
            this.jitterFactor = jitterFactor;
            this.tpsFactor = tpsFactor;
            this.scenarioAddon = scenarioAddon;
            this.pendingMargin = pendingMargin;
            this.scenarioTag = scenarioTag;
        }

        /** Total movement tolerance for this frame */
        public double getMovementAllowance() {
            return movementAllowance;
        }

        /** Extra reach margin for combat checks */
        public double getCombatReachMargin() {
            return combatReachMargin;
        }

        /** Slack window for velocity response validation */
        public double getVelocityResponseSlack() {
            return velocityResponseSlack;
        }

        public double getRttMs() {
            return rttMs;
        }

        public double getJitterMs() {
            return jitterMs;
        }

        public double getTps() {
            return tps;
        }

        public String getScenarioTag() {
            return scenarioTag;
        }

        /**
         * Debug-friendly one-line summary of budget breakdown.
         */
        public String toDebugString() {
            return String.format(Locale.ROOT,
                    "budget[move=%.4f reach=%.4f vel=%.4f] inputs[rtt=%.0fms jitter=%.0fms tps=%.1f] " +
                            "factors[lat=%.3f jit=%.3f tps=%.3f scenario=%s addon=%.4f pending=%.4f]",
                    movementAllowance, combatReachMargin, velocityResponseSlack,
                    rttMs, jitterMs, tps,
                    latencyFactor, jitterFactor, tpsFactor,
                    scenarioTag, scenarioAddon, pendingMargin);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Config interface (adapter to plugin config)
    // ═══════════════════════════════════════════════════════════════════

    public interface ConfigProvider {
        double getBaseBudget();

        double getRecentVelocityBudget();

        double getStuckSpeedBudget();

        double getLiquidBudget();

        double getGlitchyBlockBudget();

        double getPointThreeBudget();

        double getRodPullBudget();

        double getEntityCollisionBudget();

        double getHighFallBudget();

        double getTeleportBudget();

        double getReachExtraDistance();

        double getSpeedToleranceMultiplier();

        double getSmallMargin();

        double getPendingStateMargin();
    }

    /**
     * Default ConfigProvider backed by Bukkit config.
     */
    public static ConfigProvider fromBukkitConfig(final org.bukkit.configuration.file.FileConfiguration config) {
        return new ConfigProvider() {
            @Override
            public double getBaseBudget() {
                return config.getDouble("prediction.budget.base", 0.012D);
            }

            @Override
            public double getRecentVelocityBudget() {
                return config.getDouble("prediction.budget.recent-velocity", 0.022D);
            }

            @Override
            public double getStuckSpeedBudget() {
                return config.getDouble("prediction.budget.stuck-speed", 0.018D);
            }

            @Override
            public double getLiquidBudget() {
                return config.getDouble("prediction.budget.liquid", 0.03D);
            }

            @Override
            public double getGlitchyBlockBudget() {
                return config.getDouble("prediction.budget.near-glitchy-block", 0.016D);
            }

            @Override
            public double getPointThreeBudget() {
                return config.getDouble("prediction.budget.point-three", 0.018D);
            }

            @Override
            public double getRodPullBudget() {
                return config.getDouble("prediction.budget.rod-pull", 0.04D);
            }

            @Override
            public double getEntityCollisionBudget() {
                return config.getDouble("prediction.budget.entity-hard-collision", 0.02D);
            }

            @Override
            public double getHighFallBudget() {
                return config.getDouble("prediction.budget.high-fall-recovery", 0.05D);
            }

            @Override
            public double getTeleportBudget() {
                return config.getDouble("prediction.budget.teleport", 0.02D);
            }

            @Override
            public double getReachExtraDistance() {
                return config.getDouble("adaptive-lag.reach-extra-distance", 0.15D);
            }

            @Override
            public double getSpeedToleranceMultiplier() {
                return config.getDouble("adaptive-lag.speed-tolerance-multiplier", 1.15D);
            }

            @Override
            public double getSmallMargin() {
                return config.getDouble("adaptive-lag.speed-small-margin", 0.03D);
            }

            @Override
            public double getPendingStateMargin() {
                return config.getDouble("adaptive-lag.pending-state-margin", 0.06D);
            }
        };
    }
}
