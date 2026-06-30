package ac.grim.grimac.utils.blockstate.helper;

import com.github.retrooper.packetevents.protocol.world.BlockFace;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Contract;
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
}
