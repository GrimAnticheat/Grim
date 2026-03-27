package ac.grim.legacyac.check.impl.breaking;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.util.Vector;

public final class RotationBreakCheck extends Check {
    public RotationBreakCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "RotationBreak");
    }

    public void onBreak(BlockBreakEvent event, PlayerData data) {
        if (!isEnabled() || isExempt(event.getPlayer(), data)) {
            return;
        }
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection();
        boolean intersects = rayIntersectsAABB(
                eye.getX(), eye.getY(), eye.getZ(),
                direction.getX(), direction.getY(), direction.getZ(),
                block.getX(), block.getY(), block.getZ(),
                block.getX() + 1.0D, block.getY() + 1.0D, block.getZ() + 1.0D,
                plugin.getConfig().getDouble("checks.RotationBreak.max-reach", 5.2D));
        if (!intersects) {
            double buffer = increaseBuffer(data, 1.0D);
            if (buffer > plugin.getConfig().getDouble("checks.RotationBreak.buffer", 1.25D)) {
                flag(player, data, 1.0D, "rotation-miss");
                event.setCancelled(true);
            }
        } else {
            coolDownScore(data);
        }
    }

    public void onPacketBreak(Player player, PlayerData data, PlayerData.QueuedBlockDigSnapshot snapshot) {
        if (!isEnabled() || isExempt(player, data)) {
            return;
        }
        Location eye = new Location(player.getWorld(), snapshot.getOriginX(),
                snapshot.getOriginY() + player.getEyeHeight(), snapshot.getOriginZ(), snapshot.getYaw(),
                snapshot.getPitch());
        Vector direction = eye.getDirection();
        boolean intersects = rayIntersectsAABB(
                eye.getX(), eye.getY(), eye.getZ(),
                direction.getX(), direction.getY(), direction.getZ(),
                snapshot.getX(), snapshot.getY(), snapshot.getZ(),
                snapshot.getX() + 1.0D, snapshot.getY() + 1.0D, snapshot.getZ() + 1.0D,
                plugin.getConfig().getDouble("checks.RotationBreak.max-reach", 5.2D));
        if (!intersects) {
            double buffer = increaseBuffer(data, 1.0D);
            if (buffer > plugin.getConfig().getDouble("checks.RotationBreak.buffer", 1.25D)) {
                flag(player, data, 1.0D, "rotation-miss");
            }
        } else {
            coolDownScore(data);
        }
    }

    private boolean rayIntersectsAABB(double ox, double oy, double oz,
            double dx, double dy, double dz,
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ,
            double maxDist) {
        double tMin = 0.0D;
        double tMax = maxDist;

        if (Math.abs(dx) > 1.0E-8D) {
            double t1 = (minX - ox) / dx;
            double t2 = (maxX - ox) / dx;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return false;
        } else if (ox < minX || ox > maxX) {
            return false;
        }

        if (Math.abs(dy) > 1.0E-8D) {
            double t1 = (minY - oy) / dy;
            double t2 = (maxY - oy) / dy;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return false;
        } else if (oy < minY || oy > maxY) {
            return false;
        }

        if (Math.abs(dz) > 1.0E-8D) {
            double t1 = (minZ - oz) / dz;
            double t2 = (maxZ - oz) / dz;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return false;
        } else if (oz < minZ || oz > maxZ) {
            return false;
        }

        return true;
    }
}
