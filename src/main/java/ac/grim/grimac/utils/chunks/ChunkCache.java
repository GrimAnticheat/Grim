package ac.grim.grimac.utils.chunks;

import ac.grim.grimac.GrimAC;
import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

// Inspired by https://github.com/GeyserMC/Geyser/blob/master/connector/src/main/java/org/geysermc/connector/network/session/cache/ChunkCache.java
public class ChunkCache {
    private static final Long2ObjectMap<Chunk> chunks = new Long2ObjectOpenHashMap<>();
    private static int errorsShown = 0;

    public static void addToCache(Chunk chunk, int chunkX, int chunkZ) {
        long chunkPosition = ChunkUtils.chunkPositionToLong(chunkX, chunkZ);

        chunks.put(chunkPosition, chunk);
    }

    public static void updateBlock(int x, int y, int z, int block) {
        Chunk column = getChunk(x >> 4, z >> 4);
        if (column == null) {
            if (++errorsShown < 20) {
                GrimAC.plugin.getLogger().warning("Unable to set block! Please report stacktrace!");
                new Exception().printStackTrace();
            }

            return;
        }

        column.set(x & 0xF, y, z & 0xF, block);
    }

    public static Chunk getChunk(int chunkX, int chunkZ) {
        long chunkPosition = ChunkUtils.chunkPositionToLong(chunkX, chunkZ);
        return chunks.getOrDefault(chunkPosition, null);
    }

    public static int getBlockAt(int x, int y, int z) {
        Chunk column = getChunk(x >> 4, z >> 4);

        return column.get(x & 0xF, y, z & 0xF);
    }

    public static void removeChunk(int chunkX, int chunkZ) {
        long chunkPosition = ChunkUtils.chunkPositionToLong(chunkX, chunkZ);
        chunks.remove(chunkPosition);
    }
}
