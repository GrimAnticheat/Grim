package ac.grim.bukkit.utils.convert;

import ac.grim.bukkit.world.BukkitPlatformWorld;
import ac.grim.grimac.platform.api.permission.PermissionDefault;
import ac.grim.grimac.utils.math.Location;

public class ConversionUtils {
    public static org.bukkit.Location toBukkitLocation(Location location) {
        return new org.bukkit.Location(((BukkitPlatformWorld) location.getWorld()).getBukkitWorld(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public static PermissionDefault fromBukkitPermissionDefault(org.bukkit.permissions.PermissionDefault bukkitPermissionDefault) {
        switch (bukkitPermissionDefault) {
            case TRUE:
                return PermissionDefault.TRUE;
            case FALSE:
                return PermissionDefault.FALSE;
            case OP:
                return PermissionDefault.OP;
            default:
                throw new UnsupportedOperationException();
        }
    }

    public static org.bukkit.permissions.PermissionDefault toBukkitPermissionDefault(PermissionDefault permissionDefault) {
        switch (permissionDefault) {
            case TRUE:
                return org.bukkit.permissions.PermissionDefault.TRUE;
            case FALSE:
                return org.bukkit.permissions.PermissionDefault.FALSE;
            case OP:
                return org.bukkit.permissions.PermissionDefault.OP;
            default:
                throw new UnsupportedOperationException();
        }
    }
}
