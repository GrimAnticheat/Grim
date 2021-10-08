package ac.grim.grimac.utils.chunkdata.seven;

import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.blockstate.MagicBlockState;
import ac.grim.grimac.utils.chunkdata.BaseChunk;

public class SevenChunk implements BaseChunk {
    private final ByteArray3d blocks;
    private final NibbleArray3d extendedBlocks;

    public SevenChunk() {
        blocks = new ByteArray3d(4096);
        extendedBlocks = new NibbleArray3d(4096);
    }

    // I can't figure out how to remove the if statement, but at least setting is less common than getting
    @Override
    public void set(int x, int y, int z, int combinedID) {
        blocks.set(x, y, z, combinedID & 0xFF);
        extendedBlocks.set(x, y, z, combinedID >> 12);
    }

    @Override
    public BaseBlockState get(int x, int y, int z) {
        return new MagicBlockState(blocks.get(x, y, z), extendedBlocks.get(x, y, z));
    }

    // This method only works post-flattening
    // This is due to the palette system
    @Override
    public boolean isKnownEmpty() {
        return false;
    }

    public ByteArray3d getBlocks() {
        return blocks;
    }

    public NibbleArray3d getMetadata() {
        return extendedBlocks;
    }
}
