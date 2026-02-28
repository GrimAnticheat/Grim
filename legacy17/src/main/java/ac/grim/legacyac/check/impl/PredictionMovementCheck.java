package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.prediction.LegacyPredictionEngine;
import ac.grim.legacyac.prediction.PredictionResult;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

public final class PredictionMovementCheck extends Check {
    public PredictionMovementCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "Prediction");
    }

    public void onMove(PlayerMoveEvent event, PlayerData data) {
        if (!isEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        if (isExempt(player, data) || player.isFlying() || player.getVehicle() != null) {
            return;
        }

        if (event.getTo() == null) {
            return;
        }

        Material feet = event.getTo().getBlock().getType();
        Material below = event.getTo().clone().add(0.0D, -1.0D, 0.0D).getBlock().getType();
        PredictionResult result = LegacyPredictionEngine.predict(player, feet, below, data.getLastDeltaY());

        double horizontal = data.getLastDeltaXZ();
        double deltaY = data.getLastDeltaY();

        boolean badHorizontal = horizontal > result.getMaxHorizontal();
        boolean badVertical = deltaY < result.getMinVertical() || deltaY > result.getMaxVertical();

        if (badHorizontal || badVertical) {
            double score = 0.0D;
            if (badHorizontal) {
                score += horizontal - result.getMaxHorizontal();
            }
            if (badVertical) {
                score += Math.min(Math.abs(deltaY - result.getMinVertical()), Math.abs(deltaY - result.getMaxVertical()));
            }

            double buffer = increaseBuffer(data, score);
            if (buffer > plugin.getConfig().getDouble("checks.Prediction.buffer", 0.3D)) {
                flag(player, data, score, "h=" + String.format("%.3f", horizontal) + "/" + String.format("%.3f", result.getMaxHorizontal())
                    + " y=" + String.format("%.3f", deltaY) + " range=" + String.format("%.3f", result.getMinVertical()) + ".."
                    + String.format("%.3f", result.getMaxVertical()));
            }
        }
    }
}
