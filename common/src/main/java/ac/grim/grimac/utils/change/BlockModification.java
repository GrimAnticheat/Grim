package ac.grim.grimac.utils.change;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;

public record BlockModification(WrappedBlockState oldBlockContents, WrappedBlockState newBlockContents,
                                Vector3i location, int tick,
                                Cause cause) {

    @Override
    public String toString() {
        return String.format(
                "BlockModification{location=%s, old=%s, new=%s, tick=%d, cause=%s}",
                location, oldBlockContents, newBlockContents, tick, cause
        );
    }

    public enum Cause {
        START_DIGGING,
        APPLY_BLOCK_CHANGES,
        HANDLE_NETTY_SYNC_TRANSACTION,
        OTHER
    }
}
