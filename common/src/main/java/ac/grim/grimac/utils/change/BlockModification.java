package ac.grim.grimac.utils.change;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import org.jetbrains.annotations.NotNull;

public record BlockModification(
        WrappedBlockState oldBlockContents,
        WrappedBlockState newBlockContents,
        int x,
        int y,
        int z,
        int tick,
        Cause cause
) {
    @Override
    public @NotNull String toString() {
        return String.format(
                "BlockModification{x=%d, y=%d, z=%d, old=%s, new=%s, tick=%d, cause=%s}",
                x, y, z, oldBlockContents, newBlockContents, tick, cause
        );
    }

    public enum Cause {
        START_DIGGING,
        APPLY_BLOCK_CHANGES,
        HANDLE_NETTY_SYNC_TRANSACTION,
        OTHER
    }
}
