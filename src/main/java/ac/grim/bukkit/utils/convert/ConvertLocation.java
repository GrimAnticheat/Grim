package ac.grim.bukkit.utils.convert;

import ac.grim.bukkit.world.BukkitPlatformWorld;
import ac.grim.grimac.utils.math.Location;

public class ConvertLocation {
    public static org.bukkit.Location toBukkit(Location location) {
        return new org.bukkit.Location(((BukkitPlatformWorld) location.getWorld()).getBukkitWorld(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }
}
