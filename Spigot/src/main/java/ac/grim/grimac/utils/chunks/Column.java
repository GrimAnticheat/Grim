package ac.grim.grimac.utils.chunks;


import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;

public class Column {
    public final int x;
    public final int z;
    public final int transaction;
    public BaseChunk[] chunks;

    public Column(int x, int z, BaseChunk[] chunks, int transaction) {
        this.chunks = chunks;
        this.x = x;
        this.z = z;
        this.transaction = transaction;
    }

    public BaseChunk[] getChunks() {
        return chunks;
    }

    // This ability was removed in 1.17 because of the extended world height
    // Therefore, the size of the chunks are ALWAYS 16!
    public void mergeChunks(BaseChunk[] toMerge) {
        for (int i = 0; i < 16; i++) {
            if (toMerge[i] != null) chunks[i] = toMerge[i];
        }
    }
}
