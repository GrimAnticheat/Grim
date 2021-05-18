package ac.grim.grimac.utils.chunkdata;

import ac.grim.grimac.utils.blockstate.BaseBlockState;

public interface BaseChunk {
    void set(int x, int y, int z, int combinedID);

    BaseBlockState get(int x, int y, int z);
}
