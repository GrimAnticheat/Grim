package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.frame.MovementFrame;
import ac.grim.legacyac.prediction.CandidateVelocity;
import ac.grim.legacyac.prediction.LegacyPredictionEngine;
import ac.grim.legacyac.prediction.PredictionResult;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.Locale;

public final class PredictionMovementCheck extends Check {
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

        if (data.isMovementUnconfirmed()) {
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
            return;
        }

        Material feet = to.getBlock().getType();
        Material below = to.clone().add(0.0D, -1.0D, 0.0D).getBlock().getType();

        PredictionResult legacyResult = LegacyPredictionEngine.predict(
                player, feet, below,
                data.getPrevDeltaY(), data.getPrevDeltaXZ(),
                data.wasOnGround());

        List<CandidateVelocity> candidates = LegacyPredictionEngine.generateResolvedCandidates(
                player, feet, below,
                data.getPrevDeltaY(), data.getPrevDeltaXZ(),
                data.wasOnGround());

        CandidateVelocity bestCandidate = null;
        double minDeviation = Double.MAX_VALUE;
        for (CandidateVelocity candidate : candidates) {
            double horizontalDeviation = horizontal - candidate.getHorizontalMagnitude();
            double verticalDeviation = deltaY - candidate.getMotionY();
            double deviation = Math.sqrt((horizontalDeviation * horizontalDeviation)
                    + (verticalDeviation * verticalDeviation));
            if (deviation < minDeviation) {
                minDeviation = deviation;
                bestCandidate = candidate;
            }
        }

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

        double adaptiveAllowance = plugin.getConfig().getDouble("prediction.candidate-base-allowance", 0.025D);

        if (isLagging(data)) {
            adaptiveAllowance += plugin.getConfig().getDouble("prediction.lag-tolerance", 0.08D);
        }

        long timeSinceVelocity = System.currentTimeMillis() - data.getLastVelocityAt();
        if (timeSinceVelocity < 1000L) {
            double kbXZ = data.getLastVelocityXZ();
            if (kbXZ > 0.0D) {
                int ticksSince = (int) (timeSinceVelocity / 50L);
                double decayedKB = kbXZ;
                for (int i = 0; i < ticksSince && i < 20; i++) {
                    decayedKB *= 0.91D;
                }
                double previousMomentum = data.getPrevDeltaXZ() * 0.91D;
                adaptiveAllowance += previousMomentum + decayedKB + 0.3D;
            }
        }

        double newScore = Math.max(0.0D, minDeviation - adaptiveAllowance);

        boolean enforceCandidateModel = plugin.getConfig().getBoolean("prediction.candidate-enforcement", false);
        double scoreToUse;
        if (enforceCandidateModel) {
            scoreToUse = newScore;
        } else {
            scoreToUse = oldScore;
            if (data.isDebugEnabled()) {
                plugin.getLogger().info("[GLAC-DEBUG] " + player.getName()
                        + " [Prediction-Parallel] oldScore=" + fmt(oldScore)
                        + " newScore=" + fmt(newScore)
                        + " minDeviation=" + fmt(minDeviation)
                        + " allowance=" + fmt(adaptiveAllowance)
                        + " best=" + (bestCandidate == null ? "none" : bestCandidate.getProfile()));
            }
        }

        if (scoreToUse > 0.0D) {
            double weight = plugin.getConfig().getDouble("checks.Prediction.window-weight", 1.0D);
            double buffer = slideAndAddScore(data, scoreToUse, weight);

            double flagThreshold = plugin.getConfig().getDouble("checks.Prediction.buffer", 1.2D);
            if (buffer > flagThreshold) {
                String detail = "score=" + fmt(scoreToUse)
                        + " old=" + fmt(oldScore)
                        + " new=" + fmt(newScore)
                        + " dev=" + fmt(minDeviation)
                        + " allowance=" + fmt(adaptiveAllowance)
                        + " best=" + (bestCandidate == null ? "none" : bestCandidate.getProfile())
                        + " h=" + fmt(horizontal) + "/" + fmt(legacyResult.getMaxHorizontal())
                        + " y=" + fmt(deltaY) + " range=" + fmt(legacyResult.getMinVertical()) + ".."
                        + fmt(legacyResult.getMaxVertical());
                flag(player, data, scoreToUse, detail);
            }
        } else {
            coolDownScore(data);
        }
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }
}
