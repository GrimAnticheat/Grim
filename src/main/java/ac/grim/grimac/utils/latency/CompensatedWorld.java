package ac.grim.grimac.utils.latency;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.chunkdata.FlatChunk;
import ac.grim.grimac.utils.chunkdata.sixteen.SixteenChunk;
import ac.grim.grimac.utils.chunks.ChunkUtils;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.collisions.types.SimpleCollisionBox;
import ac.grim.grimac.utils.data.PistonData;
import ac.grim.grimac.utils.data.PlayerChangeBlockData;
import ac.grim.grimac.utils.data.WorldChangeBlockData;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.utils.nms.NMSUtils;
import io.github.retrooper.packetevents.utils.reflection.Reflection;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

// Inspired by https://github.com/GeyserMC/Geyser/blob/master/connector/src/main/java/org/geysermc/connector/network/session/cache/ChunkCache.java
public class CompensatedWorld {
    public static final int JAVA_AIR_ID = 0;
    private static final int MIN_WORLD_HEIGHT = 0;
    private static final int MAX_WORLD_HEIGHT = 255;
    private static final Material flattenedLava = XMaterial.LAVA.parseMaterial();
    public static List<BlockData> globalPaletteToBlockData;
    public static Method getByCombinedID;

    private static final Material SEAGRASS = XMaterial.SEAGRASS.parseMaterial();
    private static final Material TALL_SEAGRASS = XMaterial.TALL_SEAGRASS.parseMaterial();
    private static final Material KELP = XMaterial.KELP.parseMaterial();
    private static final Material KELP_PLANT = XMaterial.KELP_PLANT.parseMaterial();
    private static final Material BUBBLE_COLUMN = XMaterial.BUBBLE_COLUMN.parseMaterial();
    private static final Material WATER = XMaterial.WATER.parseMaterial();


    static {
        // The global palette only exists in 1.13+, 1.12- uses magic values for everything
        if (XMaterial.isNewVersion()) {
            getByCombinedID = Reflection.getMethod(NMSUtils.blockClass, "getCombinedId", 0);

            BufferedReader paletteReader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(GrimAC.plugin.getResource(XMaterial.getVersion() + ".txt"))));
            int paletteSize = (int) paletteReader.lines().count();
            // Reset the reader after counting
            paletteReader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(GrimAC.plugin.getResource(XMaterial.getVersion() + ".txt"))));

            globalPaletteToBlockData = new ArrayList<>(paletteSize);

            String line;

            try {
                while ((line = paletteReader.readLine()) != null) {
                    // Example line:
                    // 109 minecraft:oak_wood[axis=x]
                    String number = line.substring(0, line.indexOf(" "));

                    // This is the integer used when sending chunks
                    int globalPaletteID = Integer.parseInt(number);

                    // This is the string saved from the block
                    // Generated with a script - https://gist.github.com/MWHunter/b16a21045e591488354733a768b804f4
                    // I could technically generate this on startup but that requires setting blocks in the world
                    // Would rather have a known clean file on all servers.
                    String blockString = line.substring(line.indexOf(" ") + 1);
                    org.bukkit.block.data.BlockData referencedBlockData = Bukkit.createBlockData(blockString);

                    // Link this global palette ID to the blockdata for the second part of the script
                    globalPaletteToBlockData.add(globalPaletteID, referencedBlockData);
                }
            } catch (IOException e) {
                System.out.println("Palette reading failed! Unsupported version?");
                e.printStackTrace();
            }
        }
    }

    private final Long2ObjectMap<Column> chunks = new Long2ObjectOpenHashMap<>();
    private final GrimPlayer player;
    public ConcurrentLinkedQueue<WorldChangeBlockData> worldChangedBlockQueue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<PlayerChangeBlockData> changeBlockQueue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<PistonData> pistonData = new ConcurrentLinkedQueue<>();

    public List<PistonData> activePistons = new ArrayList<>();
    public Set<PistonData> pushingPistons = new HashSet<>();

    public CompensatedWorld(GrimPlayer player) {
        this.player = player;
    }

    public void tickUpdates(int minimumTickRequiredToContinue, int lastTransactionReceived) {
        while (true) {
            PlayerChangeBlockData changeBlockData = changeBlockQueue.peek();

            if (changeBlockData == null) break;
            // The anticheat thread is behind, this event has not occurred yet
            if (changeBlockData.tick >= minimumTickRequiredToContinue) break;
            changeBlockQueue.poll();

            player.compensatedWorld.updateBlock(changeBlockData.blockX, changeBlockData.blockY, changeBlockData.blockZ, changeBlockData.blockData);
        }

        while (true) {
            WorldChangeBlockData changeBlockData = worldChangedBlockQueue.peek();

            if (changeBlockData == null) break;
            // The player hasn't gotten this update yet
            if (changeBlockData.tick > lastTransactionReceived) {
                break;
            }

            worldChangedBlockQueue.poll();

            player.compensatedWorld.updateBlock(changeBlockData.blockX, changeBlockData.blockY, changeBlockData.blockZ, changeBlockData.blockID);
        }

        while (true) {
            PistonData data = pistonData.peek();

            if (data == null) break;

            // The player hasn't gotten this update yet
            if (data.lastTransactionSent > lastTransactionReceived) {
                break;
            }

            for (SimpleCollisionBox box : data.boxes) {
                if (player.boundingBox.isCollided(box)) {
                    data.lastTickInPushZone = true;
                }
            }

            pistonData.poll();
            activePistons.add(data);
        }
    }

    public void tickPlayerInPistonPushingArea() {
        pushingPistons.clear();

        for (PistonData data : activePistons) {
            data.lastTickInPushZone = data.thisTickPushingPlayer;
            data.thisTickPushingPlayer = false;

            for (SimpleCollisionBox box : data.boxes) {
                if (player.boundingBox.isCollided(box)) {
                    pushingPistons.add(data);
                    data.thisTickPushingPlayer = true;
                }
            }

            data.hasPlayerRemainedInPushZone = data.hasPlayerRemainedInPushZone && data.thisTickPushingPlayer;
        }

        // Tick the pistons and remove them if they can no longer exist
        activePistons.removeIf(PistonData::tickIfGuaranteedFinished);
    }

    public boolean isChunkLoaded(int chunkX, int chunkZ) {
        long chunkPosition = ChunkUtils.chunkPositionToLong(chunkX, chunkZ);

        return chunks.containsKey(chunkPosition);
    }

    public void addToCache(Column chunk, int chunkX, int chunkZ) {
        long chunkPosition = ChunkUtils.chunkPositionToLong(chunkX, chunkZ);

        chunks.put(chunkPosition, chunk);
    }

    public void updateBlock(int x, int y, int z, BlockData blockData) {
        updateBlock(x, y, z, globalPaletteToBlockData.indexOf(blockData));
    }

    public void updateBlock(int x, int y, int z, int block) {
        Column column = getChunk(x >> 4, z >> 4);

        try {
            FlatChunk chunk = column.getChunks()[y >> 4];
            if (chunk == null) {
                column.getChunks()[y >> 4] = new SixteenChunk();
                chunk = column.getChunks()[y >> 4];

                // Sets entire chunk to air
                // This glitch/feature occurs due to the palette size being 0 when we first create a chunk section
                // Meaning that all blocks in the chunk will refer to palette #0, which we are setting to air
                chunk.set(0, 0, 0, 0);
            }

            chunk.set(x & 0xF, y & 0xF, z & 0xF, block);
        } catch (Exception e) {
            GrimAC.plugin.getLogger().warning("Unable to get set block data for chunk x " + (x >> 4) + " z " + (z >> 4));
        }
    }

    public Column getChunk(int chunkX, int chunkZ) {
        long chunkPosition = ChunkUtils.chunkPositionToLong(chunkX, chunkZ);
        return chunks.getOrDefault(chunkPosition, null);
    }

    public BlockData getBukkitBlockDataAt(double x, double y, double z) {
        return getBukkitBlockDataAt((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    public Material getBukkitMaterialAt(double x, double y, double z) {
        return getBukkitBlockDataAt((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)).getMaterial();
    }

    public BlockData getBukkitBlockDataAt(int x, int y, int z) {
        Column column = getChunk(x >> 4, z >> 4);

        if (y < MIN_WORLD_HEIGHT || y > MAX_WORLD_HEIGHT) return globalPaletteToBlockData.get(JAVA_AIR_ID);

        try {
            FlatChunk chunk = column.getChunks()[y >> 4];
            if (chunk != null) {
                return globalPaletteToBlockData.get(chunk.get(x & 0xF, y & 0xF, z & 0xF));
            }
        } catch (Exception e) {
            GrimAC.plugin.getLogger().warning("Unable to get block data from chunk x " + (x >> 4) + " z " + (z >> 4));
        }

        return globalPaletteToBlockData.get(JAVA_AIR_ID);
    }

    public double getFluidLevelAt(double x, double y, double z) {
        return getFluidLevelAt((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    public double getFluidLevelAt(int x, int y, int z) {
        return Math.max(getWaterFluidLevelAt(x, y, z), getLavaFluidLevelAt(x, y, z));
    }

    // 1.13+ only
    public double getLavaFluidLevelAt(int x, int y, int z) {
        BlockData bukkitBlock = getBukkitBlockDataAt(x, y, z);

        if (bukkitBlock.getMaterial() == flattenedLava) {
            BlockData aboveData = getBukkitBlockDataAt(x, y + 1, z);

            if (aboveData.getMaterial() == flattenedLava) {
                return 1;
            }

            Levelled lava = (Levelled) bukkitBlock;

            // Falling lava has a level of 8
            if (lava.getLevel() >= 8) return 8 / 9f;

            // I have no clue why this is needed.
            // What the server STORES and SENDS is different from what the client f3 menu shows!
            // This is not a glitch in this software, it is a vanilla glitch we are "hacking" around
            return (8 - ((Levelled) bukkitBlock).getLevel()) / 9f;
        }

        return 0;
    }

    // 1.13+ only
    public double getWaterFluidLevelAt(int x, int y, int z) {
        BlockData bukkitBlock = getBukkitBlockDataAt(x, y, z);

        if (bukkitBlock.getMaterial() == SEAGRASS || bukkitBlock.getMaterial() == TALL_SEAGRASS
                || bukkitBlock.getMaterial() == KELP || bukkitBlock.getMaterial() == KELP_PLANT ||
                bukkitBlock.getMaterial() == BUBBLE_COLUMN) {
            // This is terrible lmao
            BlockData aboveData = getBukkitBlockDataAt(x, y + 1, z);

            if (aboveData instanceof Waterlogged && ((Waterlogged) aboveData).isWaterlogged() ||
                    aboveData.getMaterial() == SEAGRASS || aboveData.getMaterial() == TALL_SEAGRASS
                    || aboveData.getMaterial() == KELP || aboveData.getMaterial() == KELP_PLANT ||
                    aboveData.getMaterial() == BUBBLE_COLUMN || bukkitBlock.getMaterial() == WATER) {
                return 1;
            }

            return 8 / 9f;
        }

        // Not sure if this is correct, but it seems so.
        if (bukkitBlock instanceof Waterlogged) {
            if (((Waterlogged) bukkitBlock).isWaterlogged()) return 8 / 9f;
        }

        if (bukkitBlock instanceof Levelled && bukkitBlock.getMaterial() == WATER) {
            int waterLevel = ((Levelled) bukkitBlock).getLevel();
            BlockData aboveData = getBukkitBlockDataAt(x, y + 1, z);

            if (aboveData instanceof Waterlogged && ((Waterlogged) aboveData).isWaterlogged() ||
                    aboveData.getMaterial() == SEAGRASS || aboveData.getMaterial() == TALL_SEAGRASS
                    || aboveData.getMaterial() == KELP || aboveData.getMaterial() == KELP_PLANT ||
                    aboveData.getMaterial() == BUBBLE_COLUMN || aboveData.getMaterial() == WATER) {
                return 1;
            }

            // Falling water has a level of 8
            if (waterLevel >= 8) return 8 / 9f;

            return (8 - waterLevel) / 9f;
        }

        return 0;
    }

    public boolean isWaterSourceBlock(int x, int y, int z) {
        BlockData bukkitBlock = getBukkitBlockDataAt(x, y, z);
        if (bukkitBlock instanceof Levelled && bukkitBlock.getMaterial() == WATER) {
            return ((Levelled) bukkitBlock).getLevel() == 0;
        }

        // These blocks are also considered source blocks
        return bukkitBlock.getMaterial() == SEAGRASS || bukkitBlock.getMaterial() == TALL_SEAGRASS
                || bukkitBlock.getMaterial() == KELP || bukkitBlock.getMaterial() == KELP_PLANT ||
                bukkitBlock.getMaterial() == BUBBLE_COLUMN;
    }

    public void removeChunk(int chunkX, int chunkZ) {
        long chunkPosition = ChunkUtils.chunkPositionToLong(chunkX, chunkZ);
        chunks.remove(chunkPosition);
    }
}
