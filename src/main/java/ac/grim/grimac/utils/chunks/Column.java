package ac.grim.grimac.utils.chunks;


import ac.grim.grimac.utils.chunkdata.sixteen.Chunk;

public class Column {
    public final int x;
    public final int z;
    public final Chunk[] chunks;

    public Column(int x, int z, Chunk[] chunks) {
        this.chunks = chunks;
        this.x = x;
        this.z = z;
    }

    public Chunk[] getChunks() {
        return chunks;
    }
}
