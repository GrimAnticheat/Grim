package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.frame.MovementFrame;
import ac.grim.legacyac.tolerance.ToleranceBudgetEngine;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Speed post-processing layer.
 *
 * Main judgement comes from prediction output (candidate input vectors +
 * uncertainty budget).
 * This check only consumes the net horizontal deviation and applies additional
 * anti-false-positive
 * gating for view/input consistency before raising alerts.
 */
public final class SpeedCheck extends Check {
    public SpeedCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "Speed");
    }

    public void onMovementFrame(Player player, MovementFrame frame, Location from, Location to, PlayerData data) {
        if (!isEnabled()) {
            return;
        }

        if (isExempt(player, data) || player.isFlying() || player.getVehicle() != null) {
            return;
        }

        if (!data.hasPredictionForFrame(frame.getTimestampNanos())) {
            return;
        }

        PlayerData.MovementStateSnapshot state = data.getMovementStateSnapshot();
        if (!state.isTeleportAligned()) {
            return;
        }

        double horizontal = data.getLastDeltaXZ();
        double deltaX = to.getX() - from.getX();
        double deltaZ = to.getZ() - from.getZ();

        double yawDelta = Math.abs(data.getLastYawDelta());
        double directionDeviation = computeDirectionDeviation(deltaX, deltaZ, to.getYaw());
        boolean directionStable = isDirectionStable(yawDelta, directionDeviation, horizontal);

        // Speed no longer maintains an independent horizontal/max single-point model.
        // The candidate predictor already covers forward/strafe combinations, including
        // sprint-jump and diagonal cases. We only consume prediction net deviations
        // here.
        double predictionHorizontalDeviation = data.getPredictionHorizontalDeviation();
        double reducedHorizontalDeviation = data.getPredictionReducedHorizontalDeviation();

        double netDeviation = reducedHorizontalDeviation;
        double baseAllowance = plugin.getConfig().getDouble("checks.Speed.prediction-base-allowance", 0.0D);
        netDeviation = Math.max(0.0D, netDeviation - baseAllowance);

        // FR-3: Use BudgetSnapshot for tolerance instead of hardcoded margins
        ToleranceBudgetEngine.BudgetSnapshot budget = getBudget(data);
        if (budget != null) {
            netDeviation = Math.max(0.0D, netDeviation - budget.getMovementAllowance());
        } else {
            // Fallback: original hardcoded tolerance logic
            if (!state.isFullyAligned()) {
                netDeviation = Math.max(0.0D, netDeviation
                        - plugin.getConfig().getDouble("adaptive-lag.pending-state-margin", 0.06D));
            } else if (isLagging(data)) {
                netDeviation = Math.max(0.0D, netDeviation
                        - plugin.getConfig().getDouble("adaptive-lag.speed-small-margin", 0.03D));
            }
        }

        if (!directionStable) {
            netDeviation = Math.max(0.0D, netDeviation
                    - plugin.getConfig().getDouble("checks.Speed.turning-direction-margin", 0.03D));
        }

        if (data.isDebugEnabled()) {
            plugin.getLogger().info("[GLAC-DEBUG] " + player.getName()
                    + " Speed post h=" + fmt(horizontal)
                    + " predDev=" + fmt(predictionHorizontalDeviation)
                    + " reduced=" + fmt(reducedHorizontalDeviation)
                    + " net=" + fmt(netDeviation)
                    + " yawDelta=" + fmt(yawDelta)
                    + " dirDev=" + fmt(directionDeviation)
                    + " dirStable=" + directionStable
                    + " pending=" + state.getPendingChanges()
                    + " best=" + data.getPredictionBestProfile()
                    + (budget != null ? " budget=" + String.format(Locale.ROOT, "%.4f", budget.getMovementAllowance())
                            + " scenario=" + budget.getScenarioTag() : ""));
        }

        if (netDeviation <= 0.0D) {
            coolDownScore(data);
            if (frame.isOnGround() && from.getY() == to.getY()) {
                data.setLastSafeLocation(to.clone());
            }
            return;
        }

        recordEvidence(data, netDeviation, "SPEED_POST_PREDICTION");
        double weight = plugin.getConfig().getDouble("checks.Speed.window-weight", 1.0D);
        double buffer = slideAndAddScore(data, netDeviation, weight);
        if (buffer > plugin.getConfig().getDouble("checks.Speed.buffer", 0.35D)) {
            flag(player, data, netDeviation, "net=" + fmt(netDeviation)
                    + " predRaw=" + fmt(predictionHorizontalDeviation)
                    + " predReduced=" + fmt(reducedHorizontalDeviation)
                    + " dirDev=" + fmt(directionDeviation)
                    + " yawDelta=" + fmt(yawDelta)
                    + " best=" + data.getPredictionBestProfile());
        }
    }

    private boolean isDirectionStable(double yawDelta, double directionDeviation, double horizontal) {
        if (horizontal < plugin.getConfig().getDouble("checks.Speed.direction-min-horizontal", 0.06D)) {
            return true;
        }
        double maxYawDelta = plugin.getConfig().getDouble("checks.Speed.turning-max-yaw-delta", 45.0D);
        double maxDirectionDeviation = plugin.getConfig().getDouble("checks.Speed.turning-max-direction-deviation",
                85.0D);
        return yawDelta <= maxYawDelta && directionDeviation <= maxDirectionDeviation;
    }

    private String fmt(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private static double computeDirectionDeviation(double deltaX, double deltaZ, float yaw) {
        double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (horizontal < 1.0E-5D) {
            return 0.0D;
        }
        double movementAngle = Math.toDegrees(Math.atan2(-deltaX, deltaZ));
        double facingAngle = normalizeAngle(yaw);
        double diff = Math.abs(movementAngle - facingAngle);
        if (diff > 180.0D) {
            diff = 360.0D - diff;
        }
        return diff;
    }

    private static double normalizeAngle(double angle) {
        double normalized = angle % 360.0D;
        if (normalized < 0.0D) {
            normalized += 360.0D;
        }
        return normalized;
    }
}
