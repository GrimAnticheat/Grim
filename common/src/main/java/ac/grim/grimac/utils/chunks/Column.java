package ac.grim.grimac.utils.chunks;


import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;

public record Column(int x, int z, BaseChunk[] chunks, int transaction) {

    // This ability was removed in 1.17 because of the extended world height
    // Therefore, the size of the chunks are ALWAYS 16!
    public void mergeChunks(BaseChunk[] toMerge) {
        for (int i = 0; i < 16; i++) {
            if (toMerge[i] != null) chunks[i] = toMerge[i];
        }
    }
}
