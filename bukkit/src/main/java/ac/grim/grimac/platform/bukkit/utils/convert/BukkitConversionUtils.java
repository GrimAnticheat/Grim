package ac.grim.grimac.platform.bukkit.utils.convert;

import ac.grim.grimac.platform.bukkit.world.BukkitPlatformWorld;
import ac.grim.grimac.platform.api.permission.PermissionDefault;
import ac.grim.grimac.utils.math.Location;
import com.github.retrooper.packetevents.protocol.player.GameMode;

public class BukkitConversionUtils {
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
}
