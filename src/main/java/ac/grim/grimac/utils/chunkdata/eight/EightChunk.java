package ac.grim.grimac.utils.chunkdata.eight;

import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.blockstate.MagicBlockState;
import ac.grim.grimac.utils.chunkdata.BaseChunk;

public class EightChunk implements BaseChunk {
    private final ShortArray3d blocks;

    public EightChunk(char[] data) {
        blocks = new ShortArray3d(data);
    }

    @Override
    public void set(int x, int y, int z, int combinedID) {
        // Usual system for storing combined ID's: F (data) F (empty) FF FF (material ID)
        // 1.8 system for storing combined ID's: F (empty) FF FF (material id) F (data)
        blocks.set(x, y, z, ((combinedID & 0xFF) << 4) | (combinedID >> 12));
    }

    @Override
    public BaseBlockState get(int x, int y, int z) {
        int data = blocks.get(x, y, z);
        return new MagicBlockState(data >> 4, data & 0xF);
    }
}
