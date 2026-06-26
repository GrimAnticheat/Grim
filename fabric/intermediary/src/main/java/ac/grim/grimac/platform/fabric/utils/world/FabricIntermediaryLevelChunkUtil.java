package ac.grim.grimac.platform.fabric.utils.world;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public final class FabricIntermediaryLevelChunkUtil {

    private FabricIntermediaryLevelChunkUtil() {}

    public static boolean hasChunkAt(Level level, int chunkX, int chunkZ) {
        return ((LevelAccessor) level).getChunkSource().hasChunk(chunkX, chunkZ);
    }
}
