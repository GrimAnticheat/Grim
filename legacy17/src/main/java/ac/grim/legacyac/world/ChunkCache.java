package ac.grim.legacyac.world;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;

public final class ChunkCache {
    private static final int WORLD_HEIGHT = 256;
    private static final int SECTION_WIDTH = 16;
    private static final int BLOCK_COUNT = SECTION_WIDTH * WORLD_HEIGHT * SECTION_WIDTH;

    private final short[] packedStates = new short[BLOCK_COUNT];
    private volatile long lastSnapshotAtMillis;

    public void snapshot(World world, int chunkX, int chunkZ) {
        if (world == null) {
            return;
        }
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            world.loadChunk(chunkX, chunkZ);
        }
        Chunk chunk = world.getChunkAt(chunkX, chunkZ);
        for (int y = 0; y < WORLD_HEIGHT; y++) {
            for (int z = 0; z < SECTION_WIDTH; z++) {
                for (int x = 0; x < SECTION_WIDTH; x++) {
                    int index = index(x, y, z);
                    Material type = chunk.getBlock(x, y, z).getType();
                    byte data = chunk.getBlock(x, y, z).getData();
                    packedStates[index] = pack(type, data);
                }
            }
        }
        lastSnapshotAtMillis = System.currentTimeMillis();
    }

    public LegacyBlockState getBlockState(int x, int y, int z) {
        if (x < 0 || x >= SECTION_WIDTH || z < 0 || z >= SECTION_WIDTH || y < 0 || y >= WORLD_HEIGHT) {
            return LegacyBlockState.AIR;
        }
        return unpack(packedStates[index(x, y, z)]);
    }

    public void setBlockState(int x, int y, int z, Material type, byte data) {
        if (x < 0 || x >= SECTION_WIDTH || z < 0 || z >= SECTION_WIDTH || y < 0 || y >= WORLD_HEIGHT) {
            return;
        }
        packedStates[index(x, y, z)] = pack(type, data);
    }

    public long getLastSnapshotAtMillis() {
        return lastSnapshotAtMillis;
    }

    private static int index(int x, int y, int z) {
        return (y << 8) | (z << 4) | x;
    }

    private static short pack(Material type, byte data) {
        int typeId = type == null ? 0 : type.getId();
        return (short) (((typeId & 0x0FFF) << 4) | (data & 0x0F));
    }

    private static LegacyBlockState unpack(short packed) {
        int typeId = (packed >> 4) & 0x0FFF;
        byte data = (byte) (packed & 0x0F);
        Material type = Material.getMaterial(typeId);
        return new LegacyBlockState(type == null ? Material.AIR : type, data);
    }
}
