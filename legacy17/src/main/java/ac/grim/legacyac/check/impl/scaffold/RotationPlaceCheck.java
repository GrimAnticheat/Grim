package ac.grim.legacyac.check.impl.scaffold;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.Vector;

/**
 * RotationPlace — Player not looking at the placed block.
 * Ported from Grim's RotationPlace check.
 *
 * Validates that the player's look direction intersects with the placed block's
 * bounding box. Scaffold hacks often place blocks without actually aiming at them.
 */
public final class RotationPlaceCheck extends Check {
    public RotationPlaceCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "RotationPlace");
    }

    public void onPlace(BlockPlaceEvent event, PlayerData data) {
        if (!isEnabled() || isExempt(event.getPlayer(), data)) {
            return;
        }

        Player player = event.getPlayer();
        Block placed = event.getBlockPlaced();
        Block against = event.getBlockAgainst();
        if (placed == null || against == null) {
            return;
        }

        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();

        // Check if the player's look direction can reach the "against" block
        // within a reasonable distance (vanilla reach = 4.5)
        double reach = plugin.getConfig().getDouble("checks.RotationPlace.max-reach", 6.0);

        // Ray-AABB intersection test against the "placed against" block
        double bx = against.getX();
        double by = against.getY();
        double bz = against.getZ();

        boolean intersects = rayIntersectsAABB(
                eyeLocation.getX(), eyeLocation.getY(), eyeLocation.getZ(),
                direction.getX(), direction.getY(), direction.getZ(),
                bx, by, bz, bx + 1.0, by + 1.0, bz + 1.0,
                reach);

        if (!intersects) {
            // Also try the placed block itself (some edge cases)
            double px = placed.getX();
            double py = placed.getY();
            double pz = placed.getZ();
            intersects = rayIntersectsAABB(
                    eyeLocation.getX(), eyeLocation.getY(), eyeLocation.getZ(),
                    direction.getX(), direction.getY(), direction.getZ(),
                    px, py, pz, px + 1.0, py + 1.0, pz + 1.0,
                    reach);
        }

        if (!intersects) {
            double buffer = increaseBuffer(data, 1.0);
            if (buffer > plugin.getConfig().getDouble("checks.RotationPlace.buffer", 2.0)) {
                float yaw = player.getLocation().getYaw();
                float pitch = player.getLocation().getPitch();
                flag(player, data, 1.0,
                        "notLookingAtBlock yaw=" + String.format("%.1f", yaw)
                                + " pitch=" + String.format("%.1f", pitch)
                                + " block=" + against.getX() + "," + against.getY() + "," + against.getZ());
            }
        }
    }

    /**
     * Slab method ray-AABB intersection test.
     */
    private boolean rayIntersectsAABB(double ox, double oy, double oz,
            double dx, double dy, double dz,
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ,
            double maxDist) {
        double tMin = 0.0;
        double tMax = maxDist;

        // X slab
        if (Math.abs(dx) > 1.0E-8) {
            double t1 = (minX - ox) / dx;
            double t2 = (maxX - ox) / dx;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return false;
        } else {
            if (ox < minX || ox > maxX) return false;
        }

        // Y slab
        if (Math.abs(dy) > 1.0E-8) {
            double t1 = (minY - oy) / dy;
            double t2 = (maxY - oy) / dy;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return false;
        } else {
            if (oy < minY || oy > maxY) return false;
        }

        // Z slab
        if (Math.abs(dz) > 1.0E-8) {
            double t1 = (minZ - oz) / dz;
            double t2 = (maxZ - oz) / dz;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return false;
        } else {
            if (oz < minZ || oz > maxZ) return false;
        }

        return true;
    }
}
