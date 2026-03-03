package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.frame.MovementFrame;
import ac.grim.legacyac.prediction.CandidateVelocity;
import ac.grim.legacyac.prediction.LegacyPredictionEngine;
import ac.grim.legacyac.prediction.PredictionEvaluation;
import ac.grim.legacyac.prediction.PredictionResult;
import ac.grim.legacyac.prediction.PredictionUncertaintyHandler;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public final class PredictionMovementCheck extends Check {
    private static final String SOFT_BUFFER_KEY = "Prediction.soft-buffer";
    private static final String HARD_STREAK_KEY = "Prediction.hard-streak";

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
            coolDownScore(data);
            data.scaleBuffer(SOFT_BUFFER_KEY, 0.9D);
            data.scaleBuffer(HARD_STREAK_KEY, 0.0D);
            return;
        }

        Material feet = to.getBlock().getType();
        Material below = to.clone().add(0.0D, -1.0D, 0.0D).getBlock().getType();
        PlayerData.PredictionContext context = data.getPredictionContext();
        int highFallRecoveryTicks = plugin.getConfig().getInt("prediction.recovery-after-high-fall-ticks", 8);

        PredictionResult legacyResult = LegacyPredictionEngine.predict(
                player, feet, below,
                data.getPrevDeltaY(), data.getPrevDeltaXZ(),
                data.wasOnGround(), context, highFallRecoveryTicks);

        double uncertaintyBudget = resolveContextBudgets(context);
        PredictionEvaluation evaluation = LegacyPredictionEngine.evaluateBestCandidate(
                player, feet, below,
                data.getPrevDeltaY(), data.getPrevDeltaXZ(),
                data.wasOnGround(), context, highFallRecoveryTicks,
                horizontal, deltaY,
                uncertaintyBudget);
        CandidateVelocity bestCandidate = evaluation.getBestCandidate();
        double minDeviation = evaluation.getRawOffset();
        data.setPredictionMinDeviation(minDeviation);

        double reducedOffset = PredictionUncertaintyHandler.reduceOffset(minDeviation, context, plugin);
        data.setPredictionReducedDeviation(reducedOffset);
        data.setPredictionBestProfile(bestCandidate == null ? "none" : bestCandidate.getProfile());

        boolean badHorizontalOld = horizontal > legacyResult.getMaxHorizontal();
        boolean badVerticalOld = deltaY < legacyResult.getMinVertical() || deltaY > legacyResult.getMaxVertical();
        double oldScore = 0.0D;
        if (badHorizontalOld) {
            oldScore += (horizontal - legacyResult.getMaxHorizontal());
        }
        if (badVerticalOld) {
            oldScore += (deltaY < legacyResult.getMinVertical())
                    ? (legacyResult.getMinVertical() - deltaY)
                    : (deltaY - legacyResult.getMaxVertical());
        }

        double baseAllowance = plugin.getConfig().getDouble("prediction.stage.base-allowance", 0.0D);
        double adaptiveAllowance = baseAllowance + uncertaintyBudget;

        if (!state.isFullyAligned()) {
            adaptiveAllowance += plugin.getConfig().getDouble("adaptive-lag.pending-state-margin", 0.06D);
        } else if (isLagging(data)) {
            adaptiveAllowance += plugin.getConfig().getDouble("prediction.lag-small-tolerance", 0.03D);
        }

        if (!state.isVelocityAligned()) {
            adaptiveAllowance += plugin.getConfig().getDouble("prediction.velocity-pending-tolerance", 0.04D);
        }

        logAdaptiveLagComparison(player, data, getName(), baseAllowance, adaptiveAllowance,
                "prediction-state-aligned=" + state.isFullyAligned());

        double newScore = Math.max(0.0D, reducedOffset - baseAllowance);
        if (minDeviation > 0.0D) {
            recordEvidence(data, minDeviation, "PREDICTION_MODEL");
        }

        boolean enforceCandidateModel = plugin.getConfig().getBoolean("prediction.candidate-enforcement", false);
        double scoreToUse = enforceCandidateModel ? newScore : oldScore;
        if (!enforceCandidateModel && data.isDebugEnabled()) {
            plugin.getLogger().info("[GLAC-DEBUG] " + player.getName()
                    + " [Prediction-Parallel] oldScore=" + fmt(oldScore)
                    + " newScore=" + fmt(newScore)
                    + " minDeviation=" + fmt(minDeviation)
                    + " allowance=" + fmt(adaptiveAllowance)
                    + " pending=" + state.getPendingChanges()
                    + " best=" + (bestCandidate == null ? "none" : bestCandidate.getProfile()));
        }

        if (scoreToUse <= 0.0D) {
            coolDownScore(data);
            data.scaleBuffer(SOFT_BUFFER_KEY, 0.85D);
            data.scaleBuffer(HARD_STREAK_KEY, 0.0D);
            return;
        }

        double weight = plugin.getConfig().getDouble("checks.Prediction.window-weight", 1.0D);
        double softBuffer = data.addBuffer(SOFT_BUFFER_KEY, scoreToUse * weight);
        int hardNeedStreak = plugin.getConfig().getInt("prediction.stage.hard-flag-streak", 3);
        double softThreshold = plugin.getConfig().getDouble("prediction.stage.soft-buffer-threshold", 0.35D);

        if (softBuffer < softThreshold) {
            data.scaleBuffer(HARD_STREAK_KEY, 0.0D);
            return;
        }

        double hardStreak = data.addBuffer(HARD_STREAK_KEY, 1.0D);
        if (hardStreak < hardNeedStreak) {
            return;
        }

        double buffer = slideAndAddScore(data, scoreToUse, weight);
        double flagThreshold = plugin.getConfig().getDouble("checks.Prediction.buffer", 1.2D);
        if (buffer <= flagThreshold) {
            return;
        }

        String detail = "score=" + fmt(scoreToUse)
                + " old=" + fmt(oldScore)
                + " new=" + fmt(newScore)
                + " dev=" + fmt(minDeviation)
                + " reduced=" + fmt(reducedOffset)
                + " allowance=" + fmt(adaptiveAllowance)
                + " best=" + (bestCandidate == null ? "none" : bestCandidate.getProfile())
                + " h=" + fmt(horizontal) + "/" + fmt(legacyResult.getMaxHorizontal())
                + " y=" + fmt(deltaY) + " range=" + fmt(legacyResult.getMinVertical()) + ".."
                + fmt(legacyResult.getMaxVertical());
        flag(player, data, scoreToUse, detail);
    }

    private double resolveContextBudgets(PlayerData.PredictionContext context) {
        return PredictionUncertaintyHandler.resolveBudget(context, plugin);
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }
}
