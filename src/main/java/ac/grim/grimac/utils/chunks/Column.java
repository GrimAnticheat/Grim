package ac.grim.grimac.utils.chunks;


import ac.grim.grimac.utils.chunkdata.BaseChunk;

public class Column {
    public final int x;
    public final int z;
    public final BaseChunk[] chunks;
    public final int transaction;
    public boolean markedForRemoval = false;

    public Column(int x, int z, BaseChunk[] chunks, int transaction) {
        this.chunks = chunks;
        this.x = x;
        this.z = z;
        this.transaction = transaction;
    }

    public BaseChunk[] getChunks() {
        return chunks;
    }
}
