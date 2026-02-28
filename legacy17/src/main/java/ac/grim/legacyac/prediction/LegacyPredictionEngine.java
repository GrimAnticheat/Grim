package ac.grim.legacyac.prediction;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class LegacyPredictionEngine {
    private LegacyPredictionEngine() {
    }

    public static PredictionResult predict(Player player, Material feetBlock, Material belowBlock, double lastDeltaY) {
        double friction = 0.91D;
        if (player.isOnGround()) {
            friction = getBlockFriction(belowBlock) * 0.91D;
        }

        double baseSpeed = player.isSprinting() ? 0.130D : 0.100D;
        PotionEffect speed = findPotion(player, PotionEffectType.SPEED);
        if (speed != null) {
            baseSpeed *= (1.0D + (speed.getAmplifier() + 1) * 0.2D);
        }

        double predictedHorizontal = baseSpeed / Math.max(0.55D, friction) + 0.03D;

        if (isLiquid(feetBlock) || isLiquid(belowBlock)) {
            predictedHorizontal += 0.02D;
        }
        if (feetBlock == Material.WEB || belowBlock == Material.WEB) {
            predictedHorizontal = Math.min(predictedHorizontal, 0.08D);
        }

        double gravity = 0.08D;
        double predictedMinY = (lastDeltaY - gravity) * 0.98D - 0.07D;
        double predictedMaxY = (lastDeltaY - gravity) * 0.98D + 0.07D;

        if (player.isOnGround()) {
            predictedMinY = -0.42D;
            predictedMaxY = 0.52D;
        }

        return new PredictionResult(predictedHorizontal, predictedMinY, predictedMaxY);
    }

    private static double getBlockFriction(Material material) {
        if (material == Material.ICE || material == Material.PACKED_ICE) {
            return 0.98D;
        }
        if (material == Material.SOUL_SAND) {
            return 0.60D;
        }
        return 0.60D;
    }

    private static boolean isLiquid(Material material) {
        return material == Material.WATER || material == Material.STATIONARY_WATER || material == Material.LAVA || material == Material.STATIONARY_LAVA;
    }

    private static PotionEffect findPotion(Player player, PotionEffectType type) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().equals(type)) {
                return effect;
            }
        }
        return null;
    }
}
