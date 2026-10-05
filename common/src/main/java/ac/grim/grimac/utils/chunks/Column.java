package ac.grim.grimac.utils.chunks;


import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;

public record Column(int x, int z, BaseChunk[] chunks, long[] sectionKeys, int transaction) {

    // This ability was removed in 1.17 because of the extended world height
    // Therefore, the size of the chunks are ALWAYS 16!
    //
    // sectionKeys[i] is the ChunkSectionCache key for chunks[i]: 0 when not shared.
    // Must always be updated in lockstep with chunks[].
    public void mergeChunks(BaseChunk[] toMerge, long[] keysToMerge) {
        for (int i = 0; i < 16 && i < chunks.length && i < toMerge.length; i++) {
            if (toMerge[i] != null) {
                chunks[i] = toMerge[i];
                sectionKeys[i] = keysToMerge[i];
            }
        }
    }
}
