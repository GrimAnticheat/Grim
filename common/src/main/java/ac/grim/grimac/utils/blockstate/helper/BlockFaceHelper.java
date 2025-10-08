package ac.grim.grimac.utils.blockstate.helper;

import com.github.retrooper.packetevents.protocol.world.BlockFace;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class BlockFaceHelper {
    @Contract(pure = true)
    public static boolean isFaceVertical(@Nullable BlockFace face) {
        return face == BlockFace.UP || face == BlockFace.DOWN;
    }

    @Contract(pure = true)
    public static boolean isFaceHorizontal(@Nullable BlockFace face) {
        return face == BlockFace.NORTH || face == BlockFace.EAST || face == BlockFace.SOUTH || face == BlockFace.WEST;
    }

    @Contract(pure = true)
    public static BlockFace getClockWise(@NotNull BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.EAST;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> BlockFace.SOUTH;
        };
    }

    @Contract(pure = true)
    public static BlockFace getPEClockWise(@NotNull BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.EAST;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> BlockFace.SOUTH;
        };
    }

    @Contract(pure = true)
    public static BlockFace getCounterClockwise(@NotNull BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.WEST;
            case SOUTH -> BlockFace.EAST;
            case WEST -> BlockFace.SOUTH;
            default -> BlockFace.NORTH;
        };
    }
}
