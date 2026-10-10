package ac.grim.grimac.utils.chunks;


import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;

public record Column(int x, int z, BaseChunk[] chunks, long[] sectionKeys, int transaction) {
}
