package ac.grim.grimac.utils.data;

import com.github.retrooper.packetevents.util.Vector3i;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public record MainSupportingBlockData(@Nullable Vector3i blockPos, boolean onGround) {
    public static final MainSupportingBlockData AIR_OFF_GROUND = new MainSupportingBlockData(null, false);
    public static final MainSupportingBlockData AIR_ON_GROUND = new MainSupportingBlockData(null, true);

    @Contract(pure = true)
    public boolean lastOnGroundAndNoBlock() {
        return blockPos == null && onGround;
    }
}
