package ac.grim.grimac.utils.chunks;

import ac.grim.grimac.GrimAC;
import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import net.minecraft.server.v1_16_R3.Block;
import net.minecraft.server.v1_16_R3.IBlockData;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

// Inspired by https://github.com/GeyserMC/Geyser/blob/master/connector/src/main/java/org/geysermc/connector/network/session/cache/ChunkCache.java
public class ChunkCache {
    public static final int JAVA_AIR_ID = 0;
    private static int errorsShown = 0;
    private static final Long2ObjectMap<Column> chunks = new Long2ObjectOpenHashMap<>();

    public static void addToCache(Column chunk, int chunkX, int chunkZ) {
        long chunkPosition = ChunkUtils.chunkPositionToLong(chunkX, chunkZ);

        chunks.put(chunkPosition, chunk);
    }

    public static void updateBlock(int x, int y, int z, int block) {
        Column column = getChunk(x >> 4, z >> 4);
        if (column == null) {
            if (++errorsShown < 20) {
                GrimAC.plugin.getLogger().warning("Unable to set block! Please report stacktrace!");
                new Exception().printStackTrace();
            }

            return;
        }

        Chunk chunk = column.getChunks()[y >> 4];
        if (chunk != null) {
            chunk.set(x & 0xF, y & 0xF, z & 0xF, block);
        }
    }

    public static Column getChunk(int chunkX, int chunkZ) {
        long chunkPosition = ChunkUtils.chunkPositionToLong(chunkX, chunkZ);
        return chunks.getOrDefault(chunkPosition, null);
    }

    public static IBlockData getBlockDataAt(int x, int y, int z) {
        Column column = getChunk(x >> 4, z >> 4);

        Chunk chunk = column.getChunks()[y >> 4];
        if (chunk != null) {
            return Block.getByCombinedId(chunk.get(x & 0xF, y & 0xF, z & 0xF));
        }

        return Block.getByCombinedId(JAVA_AIR_ID);
    }

    public static int getBlockAt(int x, int y, int z) {
        Column column = getChunk(x >> 4, z >> 4);

        Chunk chunk = column.getChunks()[y >> 4];
        if (chunk != null) {
            return chunk.get(x & 0xF, y & 0xF, z & 0xF);
        }

        return JAVA_AIR_ID;
    }

    public static void removeChunk(int chunkX, int chunkZ) {
        long chunkPosition = ChunkUtils.chunkPositionToLong(chunkX, chunkZ);
        chunks.remove(chunkPosition);
    }
}
