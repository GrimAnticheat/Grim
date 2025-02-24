package ac.grim.grimac.utils.blockstate.helper;

import ac.grim.grimac.utils.math.Vector3dm;
import com.github.retrooper.packetevents.protocol.world.BlockFace;

public class BlockFaceHelper {
    public static boolean isFaceVertical(BlockFace face) {
        return face == BlockFace.UP || face == BlockFace.DOWN;
    }

    public static boolean isFaceHorizontal(BlockFace face) {
        return face == BlockFace.NORTH || face == BlockFace.EAST || face == BlockFace.SOUTH || face == BlockFace.WEST;
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

    public Vector3dm offset(Vector3dm toOffset, BlockFace face) {
        toOffset.setX(toOffset.getX() + face.getModX());
        toOffset.setY(toOffset.getY() + face.getModY());
        toOffset.setZ(toOffset.getZ() + face.getModZ());
        return toOffset;
    }
}
