package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;

public class CheckIfChunksLoaded {
    public static boolean isChunksUnloadedAt(GrimPlayer player, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (maxY >= player.compensatedWorld.getMinHeight() && minY < player.compensatedWorld.getMaxHeight()) {
            minX = minX >> 4;
            minZ = minZ >> 4;
            maxX = maxX >> 4;
            maxZ = maxZ >> 4;

            for (int i = minX; i <= maxX; ++i) {
                for (int j = minZ; j <= maxZ; ++j) {
                    if (player.compensatedWorld.getChunk(i, j) == null) {
                        return true;
                    }
                }
            }

            return false;
        } else {
            return true;
        }
    }
}
