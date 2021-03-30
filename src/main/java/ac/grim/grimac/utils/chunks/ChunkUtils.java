package ac.grim.grimac.utils.chunks;

public class ChunkUtils {
    public static long chunkPositionToLong(int x, int z) {
        return ((x & 0xFFFFFFFFL) << 32L) | (z & 0xFFFFFFFFL);
    }
}
