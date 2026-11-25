package ac.grim.grimac.utils.functions;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface BlockAndPositionPredicate {
    boolean test(@NotNull WrappedBlockState block, int x, int y, int z);
}
