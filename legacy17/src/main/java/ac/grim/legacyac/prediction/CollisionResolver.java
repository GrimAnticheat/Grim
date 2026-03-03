package ac.grim.legacyac.prediction;

import org.bukkit.Material;
import org.bukkit.entity.Player;

public final class CollisionResolver {
    private static final double MOVEMENT_THRESHOLD = 0.005D;

    private CollisionResolver() {
    }

    public static CandidateVelocity resolve(Player player, Material feetBlock, Material belowBlock,
            CandidateVelocity candidate, boolean onGround) {
        double x = candidate.getMotionX();
        double y = candidate.getMotionY();
        double z = candidate.getMotionZ();

        if (Math.abs(x) < MOVEMENT_THRESHOLD) {
            x = 0.0D;
        }
        if (Math.abs(y) < MOVEMENT_THRESHOLD) {
            y = 0.0D;
        }
        if (Math.abs(z) < MOVEMENT_THRESHOLD) {
            z = 0.0D;
        }

        if (feetBlock == Material.SOUL_SAND) {
            x *= 0.4D;
            z *= 0.4D;
        }

        if (feetBlock == Material.WEB || belowBlock == Material.WEB) {
            x = clamp(x, -0.05D, 0.05D);
            z = clamp(z, -0.05D, 0.05D);
            y = clamp(y, -0.05D, 0.05D);
        }

        if (isLiquid(feetBlock) || isLiquid(belowBlock)) {
            x = clamp(x, -0.14D, 0.14D);
            z = clamp(z, -0.14D, 0.14D);
            y = clamp(y, -0.12D, 0.5D);
        }

        Material bodyBlock = player.getLocation().getBlock().getType();
        if (bodyBlock == Material.LADDER || bodyBlock == Material.VINE) {
            x = clamp(x, -0.15D, 0.15D);
            z = clamp(z, -0.15D, 0.15D);
            y = clamp(y, -0.15D, 0.2D);
        }

        if (onGround && y < -0.08D) {
            y = -0.08D;
        }

        return new CandidateVelocity(candidate.getProfile(), x, y, z);
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static boolean isLiquid(Material material) {
        return material == Material.WATER || material == Material.STATIONARY_WATER
                || material == Material.LAVA || material == Material.STATIONARY_LAVA;
    }
}
