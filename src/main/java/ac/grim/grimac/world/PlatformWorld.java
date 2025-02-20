package ac.grim.grimac.world;

import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface PlatformWorld {
    boolean isChunkLoaded(int i, int i1);

    Block getBlockAt(int i, int j, int k);

    String getName();

    @Nullable UUID getUID();

    PlatformChunk getChunkAt(int currChunkX, int currChunkZ);

    boolean isLoaded();
}
