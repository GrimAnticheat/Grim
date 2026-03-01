package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.frame.MovementFrame;
import ac.grim.legacyac.prediction.LegacyPredictionEngine;
import ac.grim.legacyac.prediction.PredictionResult;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
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


        // Don't check during teleport synchronization
        if (data.isMovementUnconfirmed()) {
            return;
        }

        // Check packet freshness — skip if the movement packet is stale
        long now = System.nanoTime();
        long packetAgeNanos = now - data.getLastRawMovementPacketAt();
        long maxPacketAgeNanos = plugin.getConfig().getLong("prediction.max-packet-age-nanos", 120000000L);
        if (data.getLastRawMovementPacketAt() != 0L && packetAgeNanos > maxPacketAgeNanos) {
            return;
        }

        // Current movement deltas
        double horizontal = data.getLastDeltaXZ();
        double deltaY = data.getLastDeltaY();

        // Skip insignificant movements (below threshold → can't flag)
        double minMovingHorizontal = plugin.getConfig().getDouble("prediction.min-moving-horizontal", 0.03D);
        double minMovingVertical = plugin.getConfig().getDouble("prediction.min-moving-vertical", 0.03D);
        if (horizontal < minMovingHorizontal && Math.abs(deltaY) < minMovingVertical) {
            // Even for small movements, decay the buffer so false-positives don't linger
            coolDownScore(data);
            return;
        }

        // Get blocks at feet and below
        Material feet = to.getBlock().getType();
        Material below = to.clone().add(0.0D, -1.0D, 0.0D).getBlock().getType();

        // Run prediction with correct physics model
        // IMPORTANT: pass PREVIOUS tick's deltas as "carried velocity"
        // because lastDeltaXZ/Y are already the current tick's values
        PredictionResult result = LegacyPredictionEngine.predict(
                player, feet, below,
                data.getPrevDeltaY(), data.getPrevDeltaXZ(),
                player.isOnGround());

        // Check horizontal: is it exceeding maximum predicted horizontal?
        boolean badHorizontal = horizontal > result.getMaxHorizontal();

        // Check vertical: is it outside the predicted Y range?
        boolean badVertical = deltaY < result.getMinVertical() || deltaY > result.getMaxVertical();

        // Additional tolerance when lagging
        if (isLagging(data)) {
            // Give extra room when server is lagging
            double lagTolerance = plugin.getConfig().getDouble("prediction.lag-tolerance", 0.08D);
            if (badHorizontal && horizontal <= result.getMaxHorizontal() + lagTolerance) {
                badHorizontal = false;
            }
            if (badVertical) {
                if (deltaY >= result.getMinVertical() - lagTolerance
                        && deltaY <= result.getMaxVertical() + lagTolerance) {
                    badVertical = false;
                }
            }
        }

        // Additional tolerance after recent knockback — use ACTUAL velocity magnitude
        long timeSinceVelocity = System.currentTimeMillis() - data.getLastVelocityAt();
        if (timeSinceVelocity < 1000L) {
            double kbXZ = data.getLastVelocityXZ();
            if (kbXZ > 0.0D) {
                // Calculate decayed knockback speed
                int ticksSince = (int) (timeSinceVelocity / 50L);
                double decayedKB = kbXZ;
                for (int i = 0; i < ticksSince && i < 20; i++) {
                    decayedKB *= 0.91D;
                }
                // The client's true speed can sum previous momentum, generic sprint effects, and knockback velocity
                double previousMomentum = data.getPrevDeltaXZ() * 0.91D;
                double kbTolerance = previousMomentum + decayedKB + 0.3D;
                if (badHorizontal && horizontal <= result.getMaxHorizontal() + kbTolerance) {
                    badHorizontal = false;
                }
                if (badVertical) {
                    // Knockback also affects Y — use the stored Y component or generous tolerance
                    double yTolerance = kbTolerance * 0.5D;
                    if (deltaY >= result.getMinVertical() - yTolerance && deltaY <= result.getMaxVertical() + yTolerance) {
                        badVertical = false;
                    }
                }
            }
        }

        // If either axis is violated, calculate a score
        if (badHorizontal || badVertical) {
            double score = 0.0D;

            if (badHorizontal) {
                double hDeviation = horizontal - result.getMaxHorizontal();
                score += hDeviation;
            }
            if (badVertical) {
                double vDeviation;
                if (deltaY < result.getMinVertical()) {
                    vDeviation = result.getMinVertical() - deltaY;
                } else {
                    vDeviation = deltaY - result.getMaxVertical();
                }
                score += vDeviation;
            }

            // Use sliding window scoring (decay + accumulate)
            double weight = plugin.getConfig().getDouble("checks.Prediction.window-weight", 1.0D);
            double buffer = slideAndAddScore(data, score, weight);

            double flagThreshold = plugin.getConfig().getDouble("checks.Prediction.buffer", 1.2D);
            if (buffer > flagThreshold) {
                String detail = "h=" + fmt(horizontal) + "/" + fmt(result.getMaxHorizontal())
                        + " y=" + fmt(deltaY) + " range=" + fmt(result.getMinVertical()) + ".."
                        + fmt(result.getMaxVertical());
                flag(player, data, score, detail);
            }
        } else {
            // Normal movement — decay the buffer
            coolDownScore(data);
        }
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }
}
