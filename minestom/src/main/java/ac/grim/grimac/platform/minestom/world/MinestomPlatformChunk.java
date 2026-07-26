package ac.grim.grimac.platform.minestom.world;

import ac.grim.grimac.platform.api.world.PlatformChunk;
import net.minestom.server.instance.Chunk;
import org.jetbrains.annotations.Nullable;

/**
 * Grim {@link PlatformChunk} over a Minestom {@link Chunk}. Block ids are vanilla global
 * state ids ({@code Block#stateId()}), which line up with PacketEvents' id space.
 */
public final class MinestomPlatformChunk implements PlatformChunk {

    private final @Nullable Chunk chunk;

    public MinestomPlatformChunk(@Nullable Chunk chunk) {
        this.chunk = chunk;
    }

    @Override
    public int getBlockID(int x, int y, int z) {
        if (chunk == null) {
            return 0; // air / unloaded
        }
        return chunk.getBlock(x, y, z).stateId();
    }
}
