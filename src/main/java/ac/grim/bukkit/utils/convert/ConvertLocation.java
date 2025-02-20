package ac.grim.bukkit.utils.convert;

import ac.grim.bukkit.world.BukkitPlatformWorld;

public class ConvertLocation {
    public static org.bukkit.Location toBukkit(ac.grim.grimac.world.Location location) {
        return new org.bukkit.Location(((BukkitPlatformWorld) location.getWorld()).getBukkitWorld(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }
}
