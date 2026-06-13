package ac.grim.grimac.platform.fabric.utils.world;

import net.minecraft.world.level.Level;

public final class FabricOfficialLevelChunkUtil {

    private FabricOfficialLevelChunkUtil() {}

    public static boolean hasChunkAt(Level level, int chunkX, int chunkZ) {
        return level.getChunkSource().hasChunk(chunkX, chunkZ);
    }
}
