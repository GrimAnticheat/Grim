package ac.grim.grimac.utils.chunks;

import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

// Credit to https://github.com/GeyserMC/Geyser/blob/master/connector/src/main/java/org/geysermc/connector/network/session/cache/ChunkCache.java
// Using their class because it is MIT and we are GPL, meaning I can freely use their code.
// Additionally the anticheat is looking at Geyser compatibility in the future.
// Replaying bedrock movements as java input has potential
public class ChunkCache {
    public static final int JAVA_AIR_ID = 0;
    private static final int MINIMUM_WORLD_HEIGHT = 0;
    private final Long2ObjectMap<Column> chunks = new Long2ObjectOpenHashMap<>();

    public Column addToCache(Column chunk) {
        long chunkPosition = ChunkUtils.chunkPositionToLong(chunk.getX(), chunk.getZ());
        Column existingChunk;
        if (chunk.getBiomeData() == null // Only consider merging columns if the new chunk isn't a full chunk
                && (existingChunk = chunks.getOrDefault(chunkPosition, null)) != null) { // Column is already present in cache, we can merge with existing
            boolean changed = false;
            for (int i = 0; i < chunk.getChunks().length; i++) { // The chunks member is final, so chunk.getChunks() will probably be inlined and then completely optimized away
                if (chunk.getChunks()[i] != null) {
                    existingChunk.getChunks()[i] = chunk.getChunks()[i];
                    changed = true;
                }
            }
            return changed ? existingChunk : null;
        } else {
            chunks.put(chunkPosition, chunk);
            return chunk;
        }
    }

    public void updateBlock(int x, int y, int z, int block) {
        Column column = this.getChunk(x >> 4, z >> 4);
        if (column == null) {
            return;
        }

        if (y < MINIMUM_WORLD_HEIGHT || (y >> 4) > column.getChunks().length - 1) {
            // Y likely goes above or below the height limit of this world
            return;
        }

        Chunk chunk = column.getChunks()[y >> 4];
        if (chunk != null) {
            chunk.set(x & 0xF, y & 0xF, z & 0xF, block);
        }
    }

    public Column getChunk(int chunkX, int chunkZ) {
        long chunkPosition = ChunkUtils.chunkPositionToLong(chunkX, chunkZ);
        return chunks.getOrDefault(chunkPosition, null);
    }

    public int getBlockAt(int x, int y, int z) {
        Column column = this.getChunk(x >> 4, z >> 4);
        if (column == null) {
            return JAVA_AIR_ID;
        }

        if (y < MINIMUM_WORLD_HEIGHT || (y >> 4) > column.getChunks().length - 1) {
            // Y likely goes above or below the height limit of this world
            return JAVA_AIR_ID;
        }

        Chunk chunk = column.getChunks()[y >> 4];
        if (chunk != null) {
            return chunk.get(x & 0xF, y & 0xF, z & 0xF);
        }

        return JAVA_AIR_ID;
    }

    public void removeChunk(int chunkX, int chunkZ) {
        long chunkPosition = ChunkUtils.chunkPositionToLong(chunkX, chunkZ);
        chunks.remove(chunkPosition);
    }
}
