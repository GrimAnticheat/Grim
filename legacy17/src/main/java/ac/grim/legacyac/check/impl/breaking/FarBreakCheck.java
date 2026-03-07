package ac.grim.legacyac.check.impl.breaking;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

public final class FarBreakCheck extends Check {
    public FarBreakCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "FarBreak");
    }

    public void onBreak(BlockBreakEvent event, PlayerData data) {
        if (!isEnabled() || isExempt(event.getPlayer(), data)) {
            return;
        }
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location eye = player.getEyeLocation();
        double distance = distanceToAabb(eye.getX(), eye.getY(), eye.getZ(), block.getX(), block.getY(), block.getZ(),
                block.getX() + 1.0D, block.getY() + 1.0D, block.getZ() + 1.0D);
        double maxReach = plugin.getConfig().getDouble("checks.FarBreak.max-distance", 5.1D);
        if (isLagging(data)) {
            maxReach += 0.15D;
        }
        if (distance > maxReach) {
            double add = Math.max(0.25D, distance - maxReach);
            double buffer = increaseBuffer(data, add);
            if (buffer > plugin.getConfig().getDouble("checks.FarBreak.buffer", 1.25D)) {
                flag(player, data, add, "distance=" + String.format(java.util.Locale.ROOT, "%.3f", distance));
                event.setCancelled(true);
            }
        } else {
            coolDownScore(data);
        }
    }

    private double distanceToAabb(double px, double py, double pz,
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ) {
        double cx = clamp(px, minX, maxX);
        double cy = clamp(py, minY, maxY);
        double cz = clamp(pz, minZ, maxZ);
        double dx = px - cx;
        double dy = py - cy;
        double dz = pz - cz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
