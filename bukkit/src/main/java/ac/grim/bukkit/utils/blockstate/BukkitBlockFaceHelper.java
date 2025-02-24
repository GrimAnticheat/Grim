package ac.grim.bukkit.utils.blockstate;

import com.github.retrooper.packetevents.protocol.world.BlockFace;

public class BukkitBlockFaceHelper {
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
