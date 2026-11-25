package ac.grim.grimac.platform.fabric.utils.world;

import lombok.experimental.UtilityClass;
import net.minecraft.world.level.Level;

@UtilityClass
public final class FabricOfficialLevelChunkUtil {
    public static boolean hasChunkAt(Level level, int chunkX, int chunkZ) {
        return level.getChunkSource().hasChunk(chunkX, chunkZ);
    }
}
