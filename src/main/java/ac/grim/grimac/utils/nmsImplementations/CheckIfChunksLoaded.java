package ac.grim.grimac.utils.nmsImplementations;

import org.bukkit.World;

public class CheckIfChunksLoaded {
    public static boolean hasChunksAt(World world, int n, int n2, int n3, int n4, int n5, int n6) {
        if (n5 < 0 || n2 >= 256) {
            return false;
        }
        n3 >>= 4;
        n6 >>= 4;
        for (int i = n >> 4; i <= (n4 >>= 4); ++i) {
            for (int j = n3; j <= n6; ++j) {
                if (world.isChunkLoaded(i, j)) continue;
                return false;
            }
        }
        return true;
    }
}
