package ac.grim.grimac.utils.chunks;


import ac.grim.grimac.utils.chunkdata.BaseChunk;

public class Column {
    public final int x;
    public final int z;
    public final BaseChunk[] chunks;

    public Column(int x, int z, BaseChunk[] chunks) {
        this.chunks = chunks;
        this.x = x;
        this.z = z;
    }

    public BaseChunk[] getChunks() {
        return chunks;
    }
}
