package ac.grim.grimac.utils.data;

import com.github.retrooper.packetevents.util.Vector3i;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public record MainSupportingBlockData(@Nullable Vector3i blockPos, boolean onGround) {
    @Contract(pure = true)
    public boolean lastOnGroundAndNoBlock() {
        return blockPos == null && onGround;
    }
}
