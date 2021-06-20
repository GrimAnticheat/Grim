package ac.grim.grimac.utils.chunkdata.seven;

import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.blockstate.MagicBlockState;
import ac.grim.grimac.utils.chunkdata.BaseChunk;

// A lot of code here taken from decompiled bukkit 1.7
public class SevenChunk implements BaseChunk {
    private final short[] blockids;
    private final byte[] blockdata;

    public SevenChunk(short[] blockids, byte[] blockdata) {
        this.blockids = blockids;
        this.blockdata = blockdata;
    }

    public final int getBlockTypeId(final int x, final int y, final int z) {
        return this.blockids[(y & 0xF) << 8 | z << 4 | x];
    }

    public final int getBlockData(final int x, final int y, final int z) {
        final int off = (y & 0xF) << 7 | z << 3 | x >> 1;
        return this.blockdata[off] >> ((x & 0x1) << 2) & 0xF;
    }

    // I can't figure out how to remove the if statement, but at least setting is less common than getting
    @Override
    public void set(int x, int y, int z, int combinedID) {
        this.blockids[(y & 0xF) << 8 | z << 4 | x] = (short) (combinedID & 0xFF);

        int data = combinedID >> 12;
        final int off = (y & 0xF) << 7 | z << 3 | x >> 1;

        if ((x & 1) == 0) {
            blockdata[off] = (byte) ((blockdata[off] & 0xF0) | data);
        } else {
            blockdata[off] = (byte) ((blockdata[off] & 0xF) | (data << 4));
        }
    }

    @Override
    public BaseBlockState get(int x, int y, int z) {
        return new MagicBlockState(getBlockTypeId(x, y, z), getBlockData(x, y, z));
    }
}
