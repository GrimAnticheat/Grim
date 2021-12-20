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

    public static org.bukkit.block.BlockFace toBukkitFace(BlockFace face) {
        switch (face) {
            case NORTH:
                return org.bukkit.block.BlockFace.NORTH;
            case SOUTH:
                return org.bukkit.block.BlockFace.SOUTH;
            case WEST:
                return org.bukkit.block.BlockFace.WEST;
            case EAST:
                return org.bukkit.block.BlockFace.EAST;
            case UP:
                return org.bukkit.block.BlockFace.UP;
            case DOWN:
                return org.bukkit.block.BlockFace.DOWN;
            default:
                return org.bukkit.block.BlockFace.SELF;
        }
    }

    public static org.bukkit.block.BlockFace getClockWise(BlockFace face) {
        switch (face) {
            case NORTH:
                return org.bukkit.block.BlockFace.EAST;
            case SOUTH:
                return org.bukkit.block.BlockFace.WEST;
            case WEST:
                return org.bukkit.block.BlockFace.NORTH;
            case EAST:
            default:
                return org.bukkit.block.BlockFace.SOUTH;
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
