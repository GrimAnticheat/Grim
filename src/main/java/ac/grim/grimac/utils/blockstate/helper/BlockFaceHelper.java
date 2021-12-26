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
        switch (face) {
            case NORTH:
                return BlockFace.NORTH;
            case SOUTH:
                return BlockFace.SOUTH;
            case WEST:
                return BlockFace.WEST;
            case EAST:
                return BlockFace.EAST;
            case UP:
                return BlockFace.UP;
            case DOWN:
                return BlockFace.DOWN;
            default:
                return BlockFace.OTHER;
        }
    }

    public static BlockFace getClockWise(BlockFace face) {
        switch (face) {
            case NORTH:
                return BlockFace.EAST;
            case SOUTH:
                return BlockFace.WEST;
            case WEST:
                return BlockFace.NORTH;
            case EAST:
            default:
                return BlockFace.SOUTH;
        }
    }

    public static BlockFace getPEClockWise(BlockFace face) {
        switch (face) {
            case NORTH:
                return BlockFace.EAST;
            case SOUTH:
                return BlockFace.WEST;
            case WEST:
                return BlockFace.NORTH;
            case EAST:
            default:
                return BlockFace.SOUTH;
        }
    }

    public static BlockFace getCounterClockwise(BlockFace face) {
        switch (face) {
            case NORTH:
                return BlockFace.WEST;
            case SOUTH:
                return BlockFace.EAST;
            case WEST:
                return BlockFace.SOUTH;
            case EAST:
            default:
                return BlockFace.NORTH;
        }
    }

    public Vector offset(Vector toOffset, BlockFace face) {
        toOffset.setX(toOffset.getX() + face.getModX());
        toOffset.setY(toOffset.getY() + face.getModY());
        toOffset.setZ(toOffset.getZ() + face.getModZ());
        return toOffset;
    }
}
