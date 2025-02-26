package ac.grim.grimac.platform.bukkit.utils.convert;

import ac.grim.grimac.platform.api.permissions.PermissionDefaultValue;
import ac.grim.grimac.platform.bukkit.world.BukkitPlatformWorld;
import ac.grim.grimac.utils.math.Location;
import ac.grim.grimac.platform.api.player.GameMode;

public class BukkitConversionUtils {
    public static org.bukkit.Location toBukkitLocation(Location location) {
        return new org.bukkit.Location(((BukkitPlatformWorld) location.getWorld()).getBukkitWorld(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public static GameMode fromBukkitGameMode(org.bukkit.GameMode gameMode) {
        switch (gameMode) {
            case CREATIVE:
                return GameMode.CREATIVE;
            case SURVIVAL:
                return GameMode.SURVIVAL;
            case ADVENTURE:
                return GameMode.ADVENTURE;
            case SPECTATOR:
                return GameMode.SPECTATOR;
            default:
                throw new IllegalStateException();
        }
    }

    public static org.bukkit.GameMode toBukkitGameMode(GameMode gameMode) {
        switch (gameMode) {
            case CREATIVE:
                return org.bukkit.GameMode.CREATIVE;
            case SURVIVAL:
                return org.bukkit.GameMode.SURVIVAL;
            case ADVENTURE:
                return org.bukkit.GameMode.ADVENTURE;
            case SPECTATOR:
                return org.bukkit.GameMode.SPECTATOR;
            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Converts this enum to a Bukkit PermissionDefault.
     *
     * @return The corresponding Bukkit PermissionDefault.
     */
    public static org.bukkit.permissions.PermissionDefault toBukkitPermissionDefault(PermissionDefaultValue permissionDefaultValue) {
        switch (permissionDefaultValue) {
            case TRUE:
                return org.bukkit.permissions.PermissionDefault.TRUE;
            case FALSE:
                return org.bukkit.permissions.PermissionDefault.FALSE;
            case OP:
                return org.bukkit.permissions.PermissionDefault.OP;
            case NOT_OP:
                return org.bukkit.permissions.PermissionDefault.NOT_OP;
            default:
                throw new IllegalStateException();
        }
    }
}
