package ac.grim.grimac.utils.chunkdata;

import ac.grim.grimac.utils.blockstate.MagicBlockState;

public interface MagicChunk {
    MagicBlockState get(int x, int y, int z);

    void set(int x, int y, int z, MagicBlockState state);
}
