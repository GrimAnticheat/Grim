package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;
import java.util.Locale;

public final class VelocityCheck extends Check {
    public VelocityCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "Velocity");
    }

    public void onVelocity(PlayerVelocityEvent event, PlayerData data) {
        if (!isEnabled()) {
            return;
        }

        Vector velocity = event.getVelocity();
        if (velocity == null) {
            return;
        }

        // Only arm the window if the knockback is significant enough to measure
        double xz = Math.sqrt(velocity.getX() * velocity.getX() + velocity.getZ() * velocity.getZ());
        double minExpectedXZ = plugin.getConfig().getDouble("checks.Velocity.min-expected-xz", 0.08D);
        if (xz < minExpectedXZ && Math.abs(velocity.getY()) < 0.05D) {
            return; // Too small to reliably detect
        }

        int ticks = plugin.getConfig().getInt("checks.Velocity.window-ticks", 8);
        data.armVelocityWindow(velocity, ticks);
    }

    public void onMove(PlayerMoveEvent event, PlayerData data) {
        if (!isEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        if (isExempt(player, data)) {
            return;
        }

        // Still waiting for the velocity window to expire
        if (data.hasPendingVelocityWindow()) {
            return;
        }

        // No completed velocity window → nothing to check
        if (!data.hasCompletedVelocityWindow()) {
            return;
        }

        double expectedXZ = data.getExpectedVelocityXZ();
        double observedXZ = data.getObservedVelocityXZ();
        double expectedY = data.getExpectedVelocityY();
        double observedY = data.getObservedVelocityY();

        double xzRatio = expectedXZ > 0.01D ? (observedXZ / expectedXZ) : 1.0D;
        double yRatio = expectedY > 0.01D ? (observedY / expectedY) : 1.0D;

        if (data.isDebugEnabled()) {
            plugin.getLogger().info("[GLAC-DEBUG] " + player.getName()
                + " Velocity expXZ=" + fmt(expectedXZ) + " obsXZ=" + fmt(observedXZ)
                + " expY=" + fmt(expectedY) + " obsY=" + fmt(observedY));
        }

        double minRatioXZ = plugin.getConfig().getDouble("checks.Velocity.min-response-ratio-xz", 0.40D);
        double minRatioY = plugin.getConfig().getDouble("checks.Velocity.min-response-ratio-y", 0.25D);

        // Vanilla combat mechanics: when a player attacks while sprinting, their
        // own motion is multiplied by 0.6 (NMS EntityLiving.attackTargetEntityWithCurrentItem).
        // Combined with opposing movement input (~0.1 blocks/t), a legit player doing
        // W-tap/sprint-reset can reduce observed KB to roughly expectedXZ * 0.6 - 0.1.
        // For expected=0.85 this gives ~0.41 ratio. We use 0.25 as the floor to still
        // catch real anti-KB (21% velocity → ratio 0.21 which is below 0.25).
        boolean inCombat = (System.currentTimeMillis() - data.getLastAttackAt()) < 500L;
        if (inCombat) {
            minRatioXZ = Math.min(minRatioXZ, 0.25D);
        }

        boolean failXZ = expectedXZ >= 0.08D && observedXZ < expectedXZ * minRatioXZ;
        boolean failY = expectedY > 0.04D && observedY < expectedY * minRatioY;

        if (failXZ || failY) {
            org.bukkit.Location loc = player.getLocation();
            if (isInLiquidOrWeb(loc)) {
                failXZ = false;
                failY = false;
            } else {
                if (failXZ && isCollidingWithWall(loc, data.getExpectedVelX(), data.getExpectedVelZ())) {
                    failXZ = false;
                }
                if (failY && isCollidingWithCeiling(loc)) {
                    failY = false;
                }
            }
        }

        if (failXZ || failY) {
            double deviation = 1.0D - Math.min(xzRatio, yRatio);
            double weight = plugin.getConfig().getDouble("checks.Velocity.window-weight", 1.0D);
            double buffer = slideAndAddScore(data, deviation, weight);
            
            if (buffer > plugin.getConfig().getDouble("checks.Velocity.buffer", 1.2D)) {
                flag(player, data, deviation,
                    "expXZ=" + fmt(expectedXZ) + " obsXZ=" + fmt(observedXZ)
                        + " expY=" + fmt(expectedY) + " obsY=" + fmt(observedY));
            }
        } else {
            coolDownScore(data);
        }

        data.clearVelocityWindow();
    }

    private boolean isCollidingWithWall(org.bukkit.Location loc, double expectedX, double expectedZ) {
        org.bukkit.World world = loc.getWorld();
        double nextX = loc.getX() + expectedX;
        double nextZ = loc.getZ() + expectedZ;

        int minX = org.bukkit.util.NumberConversions.floor(nextX - 0.3);
        int maxX = org.bukkit.util.NumberConversions.floor(nextX + 0.3);
        int minZ = org.bukkit.util.NumberConversions.floor(nextZ - 0.3);
        int maxZ = org.bukkit.util.NumberConversions.floor(nextZ + 0.3);
        int minY = org.bukkit.util.NumberConversions.floor(loc.getY());
        int maxY = org.bukkit.util.NumberConversions.floor(loc.getY() + 1.8);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    if (world.getBlockAt(x, y, z).getType().isSolid()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isCollidingWithCeiling(org.bukkit.Location loc) {
        int minX = org.bukkit.util.NumberConversions.floor(loc.getX() - 0.3);
        int maxX = org.bukkit.util.NumberConversions.floor(loc.getX() + 0.3);
        int minZ = org.bukkit.util.NumberConversions.floor(loc.getZ() - 0.3);
        int maxZ = org.bukkit.util.NumberConversions.floor(loc.getZ() + 0.3);
        int headY = org.bukkit.util.NumberConversions.floor(loc.getY() + 1.8 + 0.5);

        org.bukkit.World world = loc.getWorld();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (world.getBlockAt(x, headY, z).getType().isSolid()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isInLiquidOrWeb(org.bukkit.Location loc) {
        int minX = org.bukkit.util.NumberConversions.floor(loc.getX() - 0.3);
        int maxX = org.bukkit.util.NumberConversions.floor(loc.getX() + 0.3);
        int minZ = org.bukkit.util.NumberConversions.floor(loc.getZ() - 0.3);
        int maxZ = org.bukkit.util.NumberConversions.floor(loc.getZ() + 0.3);
        int minY = org.bukkit.util.NumberConversions.floor(loc.getY());
        int maxY = org.bukkit.util.NumberConversions.floor(loc.getY() + 1.8);

        org.bukkit.World world = loc.getWorld();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    org.bukkit.Material type = world.getBlockAt(x, y, z).getType();
                    if (type == org.bukkit.Material.WATER || type == org.bukkit.Material.STATIONARY_WATER
                        || type == org.bukkit.Material.LAVA || type == org.bukkit.Material.STATIONARY_LAVA
                        || type == org.bukkit.Material.WEB) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String fmt(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
