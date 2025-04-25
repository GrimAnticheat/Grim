package ac.grim.grimac.platform.bukkit.utils.convert;

import ac.grim.grimac.platform.api.permissions.PermissionDefaultValue;
import ac.grim.grimac.platform.bukkit.world.BukkitPlatformWorld;
import ac.grim.grimac.utils.math.Location;
import com.github.retrooper.packetevents.protocol.world.BlockFace;

public class BukkitConversionUtils {
    public static org.bukkit.Location toBukkitLocation(Location location) {
        return new org.bukkit.Location(((BukkitPlatformWorld) location.getWorld()).getBukkitWorld(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    /**
     * Converts this enum to a Bukkit PermissionDefault.
     *
     * @return The corresponding Bukkit PermissionDefault.
     */
    public static org.bukkit.permissions.PermissionDefault toBukkitPermissionDefault(PermissionDefaultValue permissionDefaultValue) {
        return switch (permissionDefaultValue) {
            case TRUE -> org.bukkit.permissions.PermissionDefault.TRUE;
            case FALSE -> org.bukkit.permissions.PermissionDefault.FALSE;
            case OP -> org.bukkit.permissions.PermissionDefault.OP;
            case NOT_OP -> org.bukkit.permissions.PermissionDefault.NOT_OP;
        };
    }

    /**
     * For use with bukkit events only
     * Grim is not meant to be restrained by bukkit!
     */
    @Deprecated
    public static BlockFace fromBukkitFace(org.bukkit.block.BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.NORTH;
            case SOUTH -> BlockFace.SOUTH;
            case WEST -> BlockFace.WEST;
            case EAST -> BlockFace.EAST;
            case UP -> BlockFace.UP;
            case DOWN -> BlockFace.DOWN;
            default -> BlockFace.OTHER;
        };
    }
}
