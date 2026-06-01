package ac.grim.grimac.platform.fabric.utils.world;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

/**
 * Trampoline for chunk-source access from {@code LevelMixin}. Calling
 * {@code level.getChunkSource()} directly inside the mixin causes Mixin's
 * pre-processor to treat it as an implicit {@code @Shadow}, which fails
 * on versions where {@code getChunkSource} lives on {@code LevelAccessor}
 * (intermediary {@code class_1936}) rather than {@code Level} itself
 * ({@code class_1937}). Routing through a static call here keeps the
 * cross-class dispatch out of the mixin's view. See issue #2568.
 */
public final class LevelChunkUtil {

    private LevelChunkUtil() {}

    public static boolean hasChunkAt(Level level, int chunkX, int chunkZ) {
        return ((LevelAccessor) level).getChunkSource().hasChunk(chunkX, chunkZ);
    }
}
