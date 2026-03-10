package ac.grim.grimac.platform.bukkit.utils.convert;

import ac.grim.grimac.platform.api.permissions.PermissionDefaultValue;
import ac.grim.grimac.platform.bukkit.world.BukkitPlatformWorld;
import ac.grim.grimac.utils.math.Location;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public class BukkitConversionUtils {
    @Contract("null -> null; !null -> new")
    public static org.bukkit.Location toBukkitLocation(Location location) {
        if (location == null) return null;
        return new org.bukkit.Location(((BukkitPlatformWorld) location.getWorld()).getBukkitWorld(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    /**
     * Converts this enum to a Bukkit PermissionDefault.
     *
     * @return The corresponding Bukkit PermissionDefault.
     */
    @Contract(value = "null -> null; !null -> !null", pure = true)
    public static @Nullable PermissionDefault toBukkitPermissionDefault(@Nullable PermissionDefaultValue permissionDefaultValue) {
        if (permissionDefaultValue == null) return null;
        return switch (permissionDefaultValue) {
            case TRUE -> PermissionDefault.TRUE;
            case FALSE -> PermissionDefault.FALSE;
            case OP -> PermissionDefault.OP;
            case NOT_OP -> PermissionDefault.NOT_OP;
        };
    }

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
