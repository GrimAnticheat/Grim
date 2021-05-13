package ac.grim.grimac.utils.chunks;


import ac.grim.grimac.utils.chunkdata.FlatChunk;

public class Column {
    public final int x;
    public final int z;
    public final FlatChunk[] chunks;

    public Column(int x, int z, FlatChunk[] chunks) {
        this.chunks = chunks;
        this.x = x;
        this.z = z;
    }

    public FlatChunk[] getChunks() {
        return chunks;
    }
}
