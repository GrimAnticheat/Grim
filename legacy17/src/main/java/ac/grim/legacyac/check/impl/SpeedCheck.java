package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.frame.MovementFrame;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Speed post-processing layer.
 *
 * Consumes prediction output (reducedHorizontalDeviation) which already has
 * the uncertainty budget subtracted by PredictionMovementCheck.
 *
 * This check uses Grim's OffsetHandler accumulator pattern (advantageGained)
 * instead of per-tick scoring + independent budget subtraction.
 *
 * Key fixes vs previous version:
 * - No double budget subtraction (PredictionMovementCheck already handles it)
 * - Removed overly aggressive direction stability gating
 * - Uses advantage accumulator with configurable decay
 * - Fixed advantage to decay properly via both multiplicative and linear decay
 *   every tick regardless of offset (matching Grim's pattern where advantage only
 *   grows on offset ticks and decays on clean ticks)
 */
public final class SpeedCheck extends Check {
    /** Accumulated horizontal advantage — mirrors Grim's OffsetHandler pattern */
    private static final String ADVANTAGE_KEY = "Speed.advantage";

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

        // Reduced horizontal deviation — already has uncertainty budget subtracted
        // by PredictionUncertaintyHandler in PredictionMovementCheck
        double reducedHorizontalDeviation = data.getPredictionReducedHorizontalDeviation();

        // Apply only a small base allowance for floating-point noise
        double baseAllowance = plugin.getConfig().getDouble("checks.Speed.prediction-base-allowance", 0.0D);
        double offset = Math.max(0.0D, reducedHorizontalDeviation - baseAllowance);

        // Extra tolerance only for non-aligned state (lag/pending changes)
        // Do NOT subtract budget again — it was already subtracted in prediction
        if (!state.isEnforceable()) {
            offset = Math.max(0.0D, offset
                    - plugin.getConfig().getDouble("adaptive-lag.pending-state-margin", 0.06D));
        } else if (isLagging(data)) {
            offset = Math.max(0.0D, offset
                    - plugin.getConfig().getDouble("adaptive-lag.speed-small-margin", 0.03D));
        }

        if (data.isDebugEnabled()) {
            double horizontal = data.getLastDeltaXZ();
            plugin.getLogger().info("[GLAC-DEBUG] " + player.getName()
                    + " Speed post h=" + fmt(horizontal)
                    + " reducedHDev=" + fmt(reducedHorizontalDeviation)
                    + " offset=" + fmt(offset)
                    + " advantage=" + fmt(data.getBuffer(ADVANTAGE_KEY))
                    + " pending=" + state.getPendingChanges()
                    + " best=" + data.getPredictionBestProfile());
        }

        // Grim OffsetHandler pattern
        // Increased threshold to 0.005 to absorb 1.7.10 MathHelper/Float noise
        double threshold = plugin.getConfig().getDouble("checks.Speed.threshold", 0.005D);
        double maxAdvantage = plugin.getConfig().getDouble("checks.Speed.max-advantage", 0.5D);
        double setbackDecayMultiplier = plugin.getConfig().getDouble("checks.Speed.setback-decay-multiplier", 0.999D);
        double immediateSetbackThreshold = plugin.getConfig().getDouble("checks.Speed.immediate-setback-threshold", 0.1D);

        if (offset >= threshold) {
            double advantage = data.addBuffer(ADVANTAGE_KEY, offset);
            double maxCeiling = plugin.getConfig().getDouble("checks.Speed.max-ceiling", 2.0D);
            advantage = Math.min(advantage, maxCeiling);
            data.setBuffer(ADVANTAGE_KEY, advantage);

            recordEvidence(data, offset, "SPEED_POST_PREDICTION");

            if (advantage >= maxAdvantage || offset >= immediateSetbackThreshold) {
                flag(player, data, offset, "offset=" + fmt(offset)
                        + " adv=" + fmt(advantage)
                        + " reducedHDev=" + fmt(reducedHorizontalDeviation)
                        + " best=" + data.getPredictionBestProfile());
            }
        } else {
            // Decay advantage — multiplicative + linear (same as Prediction)
            data.scaleBuffer(ADVANTAGE_KEY, setbackDecayMultiplier);
            double linearDecay = plugin.getConfig().getDouble("checks.Speed.linear-decay-per-tick", 0.05D);
            double current = data.getBuffer(ADVANTAGE_KEY);
            if (current > 0.0D) {
                data.setBuffer(ADVANTAGE_KEY, Math.max(0.0D, current - linearDecay));
            }
            if (frame.isOnGround() && from.getY() == to.getY()) {
                data.setLastSafeLocation(to.clone());
            }
        }
    }


    public void onPredictionMissMinimal(Player player, MovementFrame frame, Location from, Location to, PlayerData data,
            double conservativeThreshold) {
        if (!isEnabled()) {
            return;
        }
        if (isExempt(player, data) || player.isFlying() || player.getVehicle() != null) {
            return;
        }
        if (!frame.hasPosition()) {
            return;
        }

        double horizontal = data.getLastDeltaXZ();
        double vertical = Math.abs(data.getLastDeltaY());
        if (horizontal <= conservativeThreshold || vertical > plugin.getConfig().getDouble("pipeline.minimal-post.thresholds.speed-max-dy", 0.42D)) {
            return;
        }

        double overflow = horizontal - conservativeThreshold;
        double increase = Math.min(1.0D, overflow * plugin.getConfig().getDouble("pipeline.minimal-post.thresholds.speed-buffer-scale", 0.35D));
        double buffer = increaseBuffer(data, increase);
        double flagBuffer = plugin.getConfig().getDouble("pipeline.minimal-post.thresholds.speed-buffer", 4.5D);

        if (data.isDebugEnabled()) {
            plugin.getLogger().info("[GLAC-DEBUG] " + player.getName()
                    + " Speed minimal post h=" + fmt(horizontal)
                    + " threshold=" + fmt(conservativeThreshold)
                    + " overflow=" + fmt(overflow)
                    + " buffer=" + fmt(buffer));
        }

        if (buffer > flagBuffer) {
            flag(player, data, overflow, "minimal-post h=" + fmt(horizontal)
                    + " threshold=" + fmt(conservativeThreshold)
                    + " overflow=" + fmt(overflow));
        }

        if (frame.isOnGround() && from.getY() == to.getY()) {
            data.setLastSafeLocation(to.clone());
        }
    }

    private String fmt(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }
}
