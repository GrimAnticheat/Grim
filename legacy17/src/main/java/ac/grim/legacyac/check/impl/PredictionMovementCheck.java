package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.frame.MovementFrame;
import ac.grim.legacyac.prediction.CandidateVelocity;
import ac.grim.legacyac.prediction.LegacyPredictionEngine;
import ac.grim.legacyac.prediction.PredictionEvaluation;
import ac.grim.legacyac.prediction.PredictionUncertaintyHandler;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Prediction check aligned with Grim's OffsetHandler (Simulation check).
 *
 * Key design from Grim:
 * - Compute best-fit candidate offset (Euclidean distance between observed and predicted)
 * - If offset >= threshold, accumulate into advantageGained
 * - If advantageGained >= maxAdvantage OR offset >= immediateSetbackThreshold → setback
 * - If offset < threshold, decay advantageGained by setbackDecayMultiplier
 * - No dual old/new scoring — only the candidate model
 */
public final class PredictionMovementCheck extends Check {
    /** Accumulated advantage — mirrors Grim's OffsetHandler.advantageGained */
    private static final String ADVANTAGE_KEY = "Prediction.advantage";

    public PredictionMovementCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "Prediction");
    }

    public void onMovementFrame(Player player, MovementFrame frame, Location to, PlayerData data) {
        if (!isEnabled()) {
            return;
        }

        if (isExempt(player, data) || player.isFlying() || player.getVehicle() != null) {
            return;
        }

        PlayerData.MovementStateSnapshot state = data.getMovementStateSnapshot();
        if (!state.isTeleportAligned()) {
            return;
        }

        long now = System.nanoTime();
        long packetAgeNanos = now - data.getLastRawMovementPacketAt();
        long maxPacketAgeNanos = plugin.getConfig().getLong("prediction.max-packet-age-nanos", 120000000L);
        if (data.getLastRawMovementPacketAt() != 0L && packetAgeNanos > maxPacketAgeNanos) {
            return;
        }

        double horizontal = data.getLastDeltaXZ();
        double deltaY = data.getLastDeltaY();

        double minMovingHorizontal = plugin.getConfig().getDouble("prediction.min-moving-horizontal", 0.03D);
        double minMovingVertical = plugin.getConfig().getDouble("prediction.min-moving-vertical", 0.03D);
        if (horizontal < minMovingHorizontal && Math.abs(deltaY) < minMovingVertical) {
            // Not moving — decay advantage, mark prediction ready
            decayAdvantage(data);
            data.markPredictionReady(frame.getTimestampNanos());
            return;
        }

        Material feet = to.getBlock().getType();
        Material below = to.clone().add(0.0D, -1.0D, 0.0D).getBlock().getType();
        PlayerData.PredictionContext context = data.getPredictionContext();
        int highFallRecoveryTicks = plugin.getConfig().getInt("prediction.recovery-after-high-fall-ticks", 8);

        // ── Uncertainty budget ──
        double uncertaintyBudget = PredictionUncertaintyHandler.resolveBudget(context, plugin);

        // ── Generate candidates and find best fit ──
        PredictionEvaluation evaluation = LegacyPredictionEngine.evaluateBestCandidate(
                player, feet, below,
                data.getPrevDeltaY(), data.getPrevDeltaXZ(),
                data.wasOnGround(), context, highFallRecoveryTicks,
                horizontal, deltaY,
                uncertaintyBudget);
        CandidateVelocity bestCandidate = evaluation.getBestCandidate();
        double rawOffset = evaluation.getRawOffset();

        // ── Store deviation data for downstream checks (Speed, etc.) ──
        data.setPredictionMinDeviation(rawOffset);
        double horizontalDeviation = 0.0D;
        if (bestCandidate != null) {
            horizontalDeviation = Math.max(0.0D, horizontal - bestCandidate.getHorizontalMagnitude());
        }
        data.setPredictionHorizontalDeviation(horizontalDeviation);
        double reducedOffset = PredictionUncertaintyHandler.reduceOffset(rawOffset, context, plugin);
        data.setPredictionReducedDeviation(reducedOffset);
        double reducedHorizontalDeviation = PredictionUncertaintyHandler.reduceOffset(horizontalDeviation, context, plugin);
        data.setPredictionReducedHorizontalDeviation(reducedHorizontalDeviation);
        data.setPredictionBestProfile(bestCandidate == null ? "none" : bestCandidate.getProfile());
        data.markPredictionReady(frame.getTimestampNanos());

        // ── Grim OffsetHandler pattern ──
        // Config mirrors Grim's Simulation section
        double threshold = plugin.getConfig().getDouble("Simulation.threshold", 0.001D);
        double immediateSetbackThreshold = plugin.getConfig().getDouble("Simulation.immediate-setback-threshold", 0.1D);
        double maxAdvantage = plugin.getConfig().getDouble("Simulation.max-advantage", 1.0D);
        double maxCeiling = plugin.getConfig().getDouble("Simulation.max-ceiling", 4.0D);
        double setbackDecayMultiplier = plugin.getConfig().getDouble("Simulation.setback-decay-multiplier", 0.999D);

        // Apply extra tolerance for non-aligned state or lag
        double extraTolerance = 0.0D;
        if (!state.isFullyAligned()) {
            extraTolerance += plugin.getConfig().getDouble("adaptive-lag.pending-state-margin", 0.06D);
        } else if (isLagging(data)) {
            extraTolerance += plugin.getConfig().getDouble("prediction.lag-small-tolerance", 0.03D);
        }

        // The effective offset after uncertainty reduction and extra tolerance
        double offset = Math.max(0.0D, reducedOffset - extraTolerance);

        if (data.isDebugEnabled()) {
            plugin.getLogger().info("[GLAC-DEBUG] " + player.getName()
                    + " [Prediction] rawOffset=" + fmt(rawOffset)
                    + " reduced=" + fmt(reducedOffset)
                    + " offset=" + fmt(offset)
                    + " threshold=" + fmt(threshold)
                    + " advantage=" + fmt(data.getBuffer(ADVANTAGE_KEY))
                    + " maxAdv=" + fmt(maxAdvantage)
                    + " h=" + fmt(horizontal)
                    + " dY=" + fmt(deltaY)
                    + " pending=" + state.getPendingChanges()
                    + " best=" + (bestCandidate == null ? "none" : bestCandidate.getProfile()));
        }

        if (offset >= threshold || offset >= immediateSetbackThreshold) {
            // Accumulate advantage
            double advantage = data.addBuffer(ADVANTAGE_KEY, offset);
            advantage = Math.min(advantage, maxCeiling);
            data.setBuffer(ADVANTAGE_KEY, advantage);

            recordEvidence(data, offset, "PREDICTION_MODEL");

            // Format offset like Grim
            String humanOffset;
            if (offset < 0.001D) {
                humanOffset = String.format(Locale.ROOT, "%.4E", offset).replace("E-0", "E-");
            } else {
                humanOffset = String.format(Locale.ROOT, "%6f", offset).replace("0.", ".");
            }

            if (advantage >= maxAdvantage || offset >= immediateSetbackThreshold) {
                // Flag and setback
                String detail = humanOffset
                        + " adv=" + fmt(advantage)
                        + " best=" + (bestCandidate == null ? "none" : bestCandidate.getProfile())
                        + " h=" + fmt(horizontal)
                        + " dY=" + fmt(deltaY);
                flag(player, data, offset, detail);
            } else {
                // Alert only (accumulating)
                plugin.alerts().alert(player, getName(), data.getViolation(getName()),
                        humanOffset + " adv=" + fmt(advantage)
                        + " best=" + (bestCandidate == null ? "none" : bestCandidate.getProfile()));
            }
        } else {
            // No significant offset — decay advantage
            decayAdvantage(data);
        }
    }

    private void decayAdvantage(PlayerData data) {
        double decayMultiplier = plugin.getConfig().getDouble("Simulation.setback-decay-multiplier", 0.999D);
        data.scaleBuffer(ADVANTAGE_KEY, decayMultiplier);
        coolDownScore(data);
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }
}
