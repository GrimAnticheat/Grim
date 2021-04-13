package ac.grim.grimac.utils.nmsImplementations;

import ac.grim.grimac.utils.chunks.ChunkCache;

public class CheckIfChunksLoaded {
    public static boolean hasChunksAt(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (maxY >= 0 && minY < 256) {
            minX = minX >> 4;
            minZ = minZ >> 4;
            maxX = maxX >> 4;
            maxZ = maxZ >> 4;

            for (int i = minX; i <= maxX; ++i) {
                for (int j = minZ; j <= maxZ; ++j) {
                    if (ChunkCache.getChunk(i, j) == null) {
                        return false;
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }
}
