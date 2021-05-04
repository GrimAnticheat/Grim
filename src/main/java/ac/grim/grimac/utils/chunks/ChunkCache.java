package ac.grim.grimac.utils.chunks;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import net.minecraft.server.v1_16_R3.Block;
import net.minecraft.server.v1_16_R3.IBlockData;
import org.apache.logging.log4j.core.util.Integers;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

// Inspired by https://github.com/GeyserMC/Geyser/blob/master/connector/src/main/java/org/geysermc/connector/network/session/cache/ChunkCache.java
public class ChunkCache {
    public static final int JAVA_AIR_ID = 0;
    private static final int MIN_WORLD_HEIGHT = 0;
    private static final int MAX_WORLD_HEIGHT = 255;
    private static final Long2ObjectMap<Column> chunks = new Long2ObjectOpenHashMap<>();
    private static final Material flattenedLava = Material.LAVA;
    public static BlockData[] globalPaletteToBlockData = new BlockData[Block.REGISTRY_ID.a()];

    public static void addToCache(Column chunk, int chunkX, int chunkZ) {
        long chunkPosition = ChunkUtils.chunkPositionToLong(chunkX, chunkZ);

        chunks.put(chunkPosition, chunk);
    }

    public static void updateBlock(int x, int y, int z, int block) {
        Column column = getChunk(x >> 4, z >> 4);

        try {
            Chunk chunk = column.getChunks()[y >> 4];
            if (chunk != null) {
                chunk.set(x & 0xF, y & 0xF, z & 0xF, block);
            }
        } catch (Exception e) {
            GrimAC.plugin.getLogger().warning("Unable to get set block data for chunk x " + (x >> 4) + " z " + (z >> 4));
        }
    }

    public static Column getChunk(int chunkX, int chunkZ) {
        long chunkPosition = ChunkUtils.chunkPositionToLong(chunkX, chunkZ);
        return chunks.getOrDefault(chunkPosition, null);
    }

    public static IBlockData getBlockDataAt(double x, double y, double z) {
        return getBlockDataAt((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    public static BlockData getBukkitBlockDataAt(double x, double y, double z) {
        return getBukkitBlockDataAt((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    public static BlockData getBukkitBlockDataAt(int x, int y, int z) {
        Column column = getChunk(x >> 4, z >> 4);

        if (y < MIN_WORLD_HEIGHT || y > MAX_WORLD_HEIGHT) return globalPaletteToBlockData[JAVA_AIR_ID];

        try {
            Chunk chunk = column.getChunks()[y >> 4];
            if (chunk != null) {
                return globalPaletteToBlockData[chunk.get(x & 0xF, y & 0xF, z & 0xF)];
            }
        } catch (Exception e) {
            GrimAC.plugin.getLogger().warning("Unable to get block data from chunk x " + (x >> 4) + " z " + (z >> 4));
        }


        return globalPaletteToBlockData[JAVA_AIR_ID];
    }

    public static IBlockData getBlockDataAt(int x, int y, int z) {
        Column column = getChunk(x >> 4, z >> 4);

        if (y < MIN_WORLD_HEIGHT || y > MAX_WORLD_HEIGHT) return Block.getByCombinedId(JAVA_AIR_ID);

        try {
            Chunk chunk = column.getChunks()[y >> 4];
            if (chunk != null) {
                return Block.getByCombinedId(chunk.get(x & 0xF, y & 0xF, z & 0xF));
            }
        } catch (Exception e) {
            GrimAC.plugin.getLogger().warning("Unable to get block data from chunk x " + (x >> 4) + " z " + (z >> 4));
        }


        return Block.getByCombinedId(JAVA_AIR_ID);
    }

    public static int getBlockAt(int x, int y, int z) {
        Column column = getChunk(x >> 4, z >> 4);

        try {
            Chunk chunk = column.getChunks()[y >> 4];
            if (chunk != null) {
                return chunk.get(x & 0xF, y & 0xF, z & 0xF);
            }
        } catch (Exception e) {
            GrimAC.plugin.getLogger().warning("Unable to get block int from chunk x " + (x >> 4) + " z " + (z >> 4));
        }

        return JAVA_AIR_ID;
    }

    public static double getFluidLevelAt(int x, int y, int z) {
        return Math.max(getWaterFluidLevelAt(x, y, z), getLavaFluidLevelAt(x, y, z));
    }

    // 1.13+ only
    public static double getLavaFluidLevelAt(int x, int y, int z) {
        BlockData bukkitBlock = getBukkitBlockDataAt(x, y, z);

        if (bukkitBlock.getMaterial() == flattenedLava) {
            BlockData aboveData = getBukkitBlockDataAt(x, y + 1, z);

            if (aboveData.getMaterial() == flattenedLava) {
                return 1;
            }

            Levelled lava = (Levelled) bukkitBlock;
            lava.getLevel();

            return ((Levelled) bukkitBlock).getLevel() / 9f;
        }

        return 0;
    }

    // I can't figure out what levels above 8 do.
    // Even on debug mode, they still can't go above level 8
    // Must have been an optimization that is no longer used
    // Doesn't work on 1.12
    public static double getWaterFluidLevelAt(int x, int y, int z) {
        BlockData bukkitBlock = getBukkitBlockDataAt(x, y, z);

        if (bukkitBlock.getMaterial() == Material.SEAGRASS || bukkitBlock.getMaterial() == Material.TALL_SEAGRASS
                || bukkitBlock.getMaterial() == Material.KELP || bukkitBlock.getMaterial() == Material.KELP_PLANT ||
                bukkitBlock.getMaterial() == Material.BUBBLE_COLUMN) {
            // This is terrible lmao
            BlockData aboveData = getBukkitBlockDataAt(x, y + 1, z);

            if (aboveData instanceof Waterlogged && ((Waterlogged) aboveData).isWaterlogged() ||
                    aboveData.getMaterial() == Material.SEAGRASS || aboveData.getMaterial() == Material.TALL_SEAGRASS
                    || aboveData.getMaterial() == Material.KELP || aboveData.getMaterial() == Material.KELP_PLANT ||
                    aboveData.getMaterial() == Material.BUBBLE_COLUMN || bukkitBlock.getMaterial() == Material.WATER) {
                return 1;
            }

            return 8 / 9f;
        }

        // Not sure if this is correct, but it seems so.
        if (bukkitBlock instanceof Waterlogged) {
            if (((Waterlogged) bukkitBlock).isWaterlogged()) return 8 / 9f;
        }

        if (bukkitBlock instanceof Levelled && bukkitBlock.getMaterial() == Material.WATER) {
            int waterLevel = ((Levelled) bukkitBlock).getLevel();
            BlockData aboveData = getBukkitBlockDataAt(x, y + 1, z);

            if (aboveData instanceof Waterlogged && ((Waterlogged) aboveData).isWaterlogged() ||
                    aboveData.getMaterial() == Material.SEAGRASS || aboveData.getMaterial() == Material.TALL_SEAGRASS
                    || aboveData.getMaterial() == Material.KELP || aboveData.getMaterial() == Material.KELP_PLANT ||
                    aboveData.getMaterial() == Material.BUBBLE_COLUMN || aboveData.getMaterial() == Material.WATER) {
                return 1;
            }

            return (8 - waterLevel) / 9f;
        }

        return 0;
    }

    public static boolean isWaterSourceBlock(int x, int y, int z) {
        BlockData bukkitBlock = getBukkitBlockDataAt(x, y, z);
        if (bukkitBlock instanceof Levelled && bukkitBlock.getMaterial() == Material.WATER) {
            return ((Levelled) bukkitBlock).getLevel() == 0;
        }

        // These blocks are also considered source blocks
        return bukkitBlock.getMaterial() == Material.SEAGRASS || bukkitBlock.getMaterial() == Material.TALL_SEAGRASS
                || bukkitBlock.getMaterial() == Material.KELP || bukkitBlock.getMaterial() == Material.KELP_PLANT ||
                bukkitBlock.getMaterial() == Material.BUBBLE_COLUMN;
    }

    public static void removeChunk(int chunkX, int chunkZ) {
        long chunkPosition = ChunkUtils.chunkPositionToLong(chunkX, chunkZ);
        chunks.remove(chunkPosition);
    }

    public static void initBlockID() {
        BufferedReader paletteReader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(GrimAC.plugin.getResource(XMaterial.getVersion() + ".txt"))));
        String line;

        try {
            while ((line = paletteReader.readLine()) != null) {
                if (!paletteReader.ready()) break;
                // Example line:
                // 109 minecraft:oak_wood[axis=x]
                String number = line.substring(0, line.indexOf(" "));

                // This is the integer used when sending chunks
                int globalPaletteID = Integers.parseInt(number);

                // This is the string saved from the block
                // Generated with a script - https://gist.github.com/MWHunter/b16a21045e591488354733a768b804f4
                // I could technically generate this on startup but that requires setting blocks in the world
                // Would rather have a known clean file on all servers.
                String blockString = line.substring(line.indexOf(" ") + 1);
                org.bukkit.block.data.BlockData referencedBlockData = Bukkit.createBlockData(blockString);

                // Link this global palette ID to the blockdata for the second part of the script
                globalPaletteToBlockData[globalPaletteID] = referencedBlockData;


            }
        } catch (IOException e) {
            System.out.println("Palette reading failed! Unsupported version?");
            e.printStackTrace();
        }
    }
}
