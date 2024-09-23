package ac.grim.grimac.utils.blockstate.helper;

import com.github.retrooper.packetevents.protocol.world.BlockFace;
import org.bukkit.util.Vector;

public class BlockFaceHelper {
    public static boolean isFaceVertical(BlockFace face) {
        return face == BlockFace.UP || face == BlockFace.DOWN;
    }

    public static boolean isFaceHorizontal(BlockFace face) {
        return face == BlockFace.NORTH || face == BlockFace.EAST || face == BlockFace.SOUTH || face == BlockFace.WEST;
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

    public static BlockFace getClockWise(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.EAST;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> BlockFace.SOUTH;
        };
    }

    public static BlockFace getPEClockWise(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.EAST;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> BlockFace.SOUTH;
        };
    }

    public static BlockFace getCounterClockwise(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.WEST;
            case SOUTH -> BlockFace.EAST;
            case WEST -> BlockFace.SOUTH;
            default -> BlockFace.NORTH;
        };
    }

    public Vector offset(Vector toOffset, BlockFace face) {
        toOffset.setX(toOffset.getX() + face.getModX());
        toOffset.setY(toOffset.getY() + face.getModY());
        toOffset.setZ(toOffset.getZ() + face.getModZ());
        return toOffset;
    }
}
