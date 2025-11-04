package ac.grim.grimac.utils.function;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;

@FunctionalInterface
public interface BlockStateAtPosPredicate {
    boolean test(WrappedBlockState state, int x, int y, int z);
}
