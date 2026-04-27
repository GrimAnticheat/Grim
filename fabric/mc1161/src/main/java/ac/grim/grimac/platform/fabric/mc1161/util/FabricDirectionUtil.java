package ac.grim.grimac.platform.fabric.mc1161.util;

import com.github.retrooper.packetevents.protocol.world.BlockFace;
import net.minecraft.core.Direction;

public final class FabricDirectionUtil {

    private FabricDirectionUtil() {}

    public static BlockFace fromDirection(Direction direction) {
        return switch (direction) {
            case NORTH -> BlockFace.NORTH;
            case SOUTH -> BlockFace.SOUTH;
            case WEST  -> BlockFace.WEST;
            case EAST  -> BlockFace.EAST;
            case UP    -> BlockFace.UP;
            case DOWN  -> BlockFace.DOWN;
        };
    }
}
