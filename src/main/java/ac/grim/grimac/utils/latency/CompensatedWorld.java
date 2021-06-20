package ac.grim.grimac.utils.latency;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.WrappedBlockData;
import ac.grim.grimac.utils.blockdata.types.WrappedBlockDataValue;
import ac.grim.grimac.utils.blockdata.types.WrappedDirectional;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.blockstate.FlatBlockState;
import ac.grim.grimac.utils.blockstate.MagicBlockState;
import ac.grim.grimac.utils.chunkdata.BaseChunk;
import ac.grim.grimac.utils.chunkdata.eight.EightChunk;
import ac.grim.grimac.utils.chunkdata.fifteen.FifteenChunk;
import ac.grim.grimac.utils.chunkdata.seven.SevenChunk;
import ac.grim.grimac.utils.chunkdata.sixteen.SixteenChunk;
import ac.grim.grimac.utils.chunkdata.twelve.TwelveChunk;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.ChangeBlockData;
import ac.grim.grimac.utils.data.PistonData;
import ac.grim.grimac.utils.data.ShulkerData;
import ac.grim.grimac.utils.data.packetentity.PacketEntityShulker;
import ac.grim.grimac.utils.nmsImplementations.Materials;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.utils.nms.NMSUtils;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.reflection.Reflection;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

// Inspired by https://github.com/GeyserMC/Geyser/blob/master/connector/src/main/java/org/geysermc/connector/network/session/cache/ChunkCache.java
public class CompensatedWorld {
    private static final int MIN_WORLD_HEIGHT = 0;
    private static final int MAX_WORLD_HEIGHT = 255;
    private static final Material WATER = XMaterial.WATER.parseMaterial();
    private static final BaseBlockState airData;
    public static List<BlockData> globalPaletteToBlockData;
    public static Method getByCombinedID;

    static {
        if (XMaterial.isNewVersion()) {
            airData = new FlatBlockState(Material.AIR.createBlockData());
        } else {
            airData = new MagicBlockState(0, 0);

        }
        // The global palette only exists in 1.13+, 1.12- uses magic values for everything
        if (XMaterial.isNewVersion()) {
            getByCombinedID = Reflection.getMethod(NMSUtils.blockClass, "getCombinedId", 0);

            BufferedReader paletteReader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(GrimAC.staticGetResource(XMaterial.getVersion() + ".txt"))));
            int paletteSize = (int) paletteReader.lines().count();
            // Reset the reader after counting
            paletteReader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(GrimAC.staticGetResource(XMaterial.getVersion() + ".txt"))));

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
    public ConcurrentLinkedQueue<ChangeBlockData> worldChangedBlockQueue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<ChangeBlockData> changeBlockQueue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<PistonData> pistonData = new ConcurrentLinkedQueue<>();

    public List<PistonData> activePistons = new ArrayList<>();
    public Set<PistonData> pushingPistons = new HashSet<>();
    public Set<ShulkerData> openShulkerBoxes = ConcurrentHashMap.newKeySet();

    public CompensatedWorld(GrimPlayer player) {
        this.player = player;
    }

    public static int getFlattenedGlobalID(BlockData blockData) {
        return globalPaletteToBlockData.indexOf(blockData);
    }

    public void tickUpdates(int lastTransactionReceived) {
        while (true) {
            ChangeBlockData changeBlockData = changeBlockQueue.peek();

            if (changeBlockData == null) break;
            // The anticheat thread is behind, this event has not occurred yet
            if (changeBlockData.transaction >= lastTransactionReceived) break;
            changeBlockQueue.poll();

            player.compensatedWorld.updateBlock(changeBlockData.blockX, changeBlockData.blockY, changeBlockData.blockZ, changeBlockData.combinedID);
        }

        while (true) {
            ChangeBlockData changeBlockData = worldChangedBlockQueue.peek();

            if (changeBlockData == null) break;
            // The player hasn't gotten this update yet
            if (changeBlockData.transaction > lastTransactionReceived) {
                break;
            }

            worldChangedBlockQueue.poll();

            player.compensatedWorld.updateBlock(changeBlockData.blockX, changeBlockData.blockY, changeBlockData.blockZ, changeBlockData.combinedID);
        }

        while (true) {
            PistonData data = pistonData.peek();

            if (data == null) break;

            // The player hasn't gotten this update yet
            if (data.lastTransactionSent > lastTransactionReceived) {
                break;
            }

            pistonData.poll();
            activePistons.add(data);
        }
    }

    public void updateBlock(int x, int y, int z, int combinedID) {
        Column column = getChunk(x >> 4, z >> 4);

        try {
            BaseChunk chunk = column.getChunks()[y >> 4];
            if (chunk == null) {
                if (XMaterial.getVersion() > 15) {
                    column.getChunks()[y >> 4] = new SixteenChunk();
                } else if (XMaterial.isNewVersion()) {
                    column.getChunks()[y >> 4] = new FifteenChunk();
                } else if (XMaterial.getVersion() > 8) {
                    column.getChunks()[y >> 4] = new TwelveChunk();
                } else if (XMaterial.getVersion() == 8){
                    column.getChunks()[y >> 4] = new EightChunk(new char[4096]);
                } else {
                    column.getChunks()[y >> 4] = new SevenChunk(new short[4096], new byte[2048]);
                }

                chunk = column.getChunks()[y >> 4];

                // Sets entire chunk to air
                // This glitch/feature occurs due to the palette size being 0 when we first create a chunk section
                // Meaning that all blocks in the chunk will refer to palette #0, which we are setting to air
                chunk.set(0, 0, 0, 0);
            }

            chunk.set(x & 0xF, y & 0xF, z & 0xF, combinedID);
        } catch (Exception ignored) {
        }
    }

    public Column getChunk(int chunkX, int chunkZ) {
        long chunkPosition = chunkPositionToLong(chunkX, chunkZ);
        return chunks.get(chunkPosition);
    }

    public static long chunkPositionToLong(int x, int z) {
        return ((x & 0xFFFFFFFFL) << 32L) | (z & 0xFFFFFFFFL);
    }

    public void tickPlayerInPistonPushingArea() {
        pushingPistons.clear();
        player.uncertaintyHandler.reset();
        // Occurs on player login
        if (player.boundingBox == null) return;
        SimpleCollisionBox playerBox = player.boundingBox.copy().expand(0.03);

        for (PistonData data : activePistons) {
            double modX = 0;
            double modY = 0;
            double modZ = 0;

            for (SimpleCollisionBox box : data.boxes) {
                if (playerBox.isCollided(box)) {
                    modX = Math.abs(data.direction.getModX()) * 1.01D;
                    modY = Math.abs(data.direction.getModY()) * 1.01D;
                    modZ = Math.abs(data.direction.getModZ()) * 1.01D;

                    playerBox.expandMax(modX, modY, modZ);
                    playerBox.expandMin(modX * -1, modY * -1, modZ * -1);
                    pushingPistons.add(data);

                    break;
                }
            }

            player.uncertaintyHandler.pistonX = Math.max(modX, player.uncertaintyHandler.pistonX);
            player.uncertaintyHandler.pistonY = Math.max(modY, player.uncertaintyHandler.pistonY);
            player.uncertaintyHandler.pistonZ = Math.max(modZ, player.uncertaintyHandler.pistonZ);
        }

        for (ShulkerData data : openShulkerBoxes) {
            double modX = 0;
            double modY = 0;
            double modZ = 0;

            SimpleCollisionBox shulkerCollision = new SimpleCollisionBox(data.position.getX(), data.position.getY(), data.position.getZ(),
                    data.position.getX() + 1, data.position.getY() + 1, data.position.getZ() + 1);

            BlockFace direction;
            if (data.entity == null) {
                BaseBlockState state = player.compensatedWorld.getWrappedBlockStateAt(data.position.getX(), data.position.getY(), data.position.getZ());
                WrappedBlockDataValue value = WrappedBlockData.getMaterialData(state);

                // This is impossible but I'm not willing to take the risk
                if (!(value instanceof WrappedDirectional)) continue;

                direction = ((WrappedDirectional) value).getDirection();
            } else {
                direction = ((PacketEntityShulker) data.entity).facing.getOppositeFace();
            }

            // Change negative corner in expansion as the direction is negative
            // We don't bother differentiating shulker entities and shulker boxes
            // I guess players can cheat to get an extra 0.49 of Y height on shulker boxes, I don't care.
            if (direction.getModX() == -1 || direction.getModY() == -1 || direction.getModZ() == -1) {
                shulkerCollision.expandMin(direction.getModX(), direction.getModY(), direction.getModZ());
            } else {
                shulkerCollision.expandMax(direction.getModZ(), direction.getModY(), direction.getModZ());
            }

            if (playerBox.isCollided(shulkerCollision)) {
                modX = Math.abs(direction.getModX());
                modY = Math.abs(direction.getModY());
                modZ = Math.abs(direction.getModZ());

                playerBox.expandMax(modX, modY, modZ);
                playerBox.expandMin(modX, modY, modZ);
            }

            player.uncertaintyHandler.pistonX = Math.max(modX, player.uncertaintyHandler.pistonX);
            player.uncertaintyHandler.pistonY = Math.max(modY, player.uncertaintyHandler.pistonY);
            player.uncertaintyHandler.pistonZ = Math.max(modZ, player.uncertaintyHandler.pistonZ);
        }

        if (activePistons.isEmpty() && openShulkerBoxes.isEmpty()) {
            player.uncertaintyHandler.pistonX = 0;
            player.uncertaintyHandler.pistonY = 0;
            player.uncertaintyHandler.pistonZ = 0;
        }

        // Tick the pistons and remove them if they can no longer exist
        activePistons.removeIf(PistonData::tickIfGuaranteedFinished);
        openShulkerBoxes.removeIf(ShulkerData::tickIfGuaranteedFinished);
    }

    public BaseBlockState getWrappedBlockStateAt(int x, int y, int z) {
        Column column = getChunk(x >> 4, z >> 4);

        if (column == null || y < MIN_WORLD_HEIGHT || y > MAX_WORLD_HEIGHT) return airData;

        BaseChunk chunk = column.getChunks()[y >> 4];
        if (chunk != null) {
            return chunk.get(x & 0xF, y & 0xF, z & 0xF);
        }

        return airData;
    }

    public boolean isChunkLoaded(int chunkX, int chunkZ) {
        long chunkPosition = chunkPositionToLong(chunkX, chunkZ);

        return chunks.containsKey(chunkPosition);
    }

    public void addToCache(Column chunk, int chunkX, int chunkZ) {
        long chunkPosition = chunkPositionToLong(chunkX, chunkZ);

        chunks.put(chunkPosition, chunk);
    }

    public Material getBukkitMaterialAt(double x, double y, double z) {
        return getWrappedBlockStateAt((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)).getMaterial();
    }

    public BaseBlockState getWrappedBlockStateAt(double x, double y, double z) {
        return getWrappedBlockStateAt((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    public double getFluidLevelAt(double x, double y, double z) {
        return getFluidLevelAt((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    public double getFluidLevelAt(int x, int y, int z) {
        return Math.max(getWaterFluidLevelAt(x, y, z), getLavaFluidLevelAt(x, y, z));
    }

    public boolean isFluidFalling(int x, int y, int z) {
        BaseBlockState bukkitBlock = getWrappedBlockStateAt(x, y, z);

        if (bukkitBlock instanceof FlatBlockState) {
            if (((FlatBlockState) bukkitBlock).getBlockData() instanceof Levelled) {
                return ((Levelled) ((FlatBlockState) bukkitBlock).getBlockData()).getLevel() > 7;
            }
        } else {
            MagicBlockState magicBlockState = (MagicBlockState) bukkitBlock;
            return ((magicBlockState.getBlockData() & 0x8) == 8);
        }

        return false;
    }

    public double getLavaFluidLevelAt(int x, int y, int z) {
        BaseBlockState bukkitBlock = getWrappedBlockStateAt(x, y, z);

        if (!Materials.checkFlag(bukkitBlock.getMaterial(), Materials.LAVA)) return 0;

        if (bukkitBlock instanceof FlatBlockState) {
            BaseBlockState aboveData = getWrappedBlockStateAt(x, y + 1, z);

            if (Materials.checkFlag(aboveData.getMaterial(), Materials.LAVA)) {
                return 1;
            }

            BlockData thisBlockData = ((FlatBlockState) bukkitBlock).getBlockData();

            if (thisBlockData instanceof Levelled) {
                // Falling lava has a level of 8
                if (((Levelled) thisBlockData).getLevel() >= 8) return 8 / 9f;

                return (8 - ((Levelled) thisBlockData).getLevel()) / 9f;
            }

        } else {
            MagicBlockState magicBlockState = (MagicBlockState) bukkitBlock;

            // If it is lava or flowing lava
            if (magicBlockState.getId() == 10 || magicBlockState.getId() == 11) {
                int magicData = magicBlockState.getData();

                // Falling lava has a level of 8
                if ((magicData & 0x8) == 8) return 8 / 9f;

                return (8 - magicData) / 9f;
            }
        }

        return 0;
    }

    public double getWaterFluidLevelAt(int x, int y, int z) {
        BaseBlockState bukkitBlock = getWrappedBlockStateAt(x, y, z);
        boolean isWater = Materials.isWater(player.getClientVersion(), bukkitBlock);

        if (!isWater) return 0;

        BaseBlockState aboveData = getWrappedBlockStateAt(x, y + 1, z);

        // If water has water above it, it's block height is 1, even if it's waterlogged
        if (Materials.isWater(player.getClientVersion(), aboveData)) {
            return 1;
        }

        if (bukkitBlock instanceof FlatBlockState) {
            FlatBlockState flatBlockState = (FlatBlockState) bukkitBlock;

            if (flatBlockState.getBlockData() instanceof Levelled) {
                if (bukkitBlock.getMaterial() == WATER) {
                    int waterLevel = ((Levelled) flatBlockState.getBlockData()).getLevel();

                    // Falling water has a level of 8
                    if (waterLevel >= 8) return 8 / 9f;

                    return (8 - waterLevel) / 9f;
                }
            }

            // The block is water, isn't water material directly, and doesn't have block above, so it is waterlogged
            // or another source-like block such as kelp.
            return 8 / 9F;
        } else {
            MagicBlockState magicBlockState = (MagicBlockState) bukkitBlock;

            // If it is water or flowing water
            if (magicBlockState.getId() == 8 || magicBlockState.getId() == 9) {
                int magicData = magicBlockState.getData();

                // Falling water has a level of 8
                if ((magicData & 0x8) == 8) return 8 / 9f;

                return (8 - magicData) / 9f;
            }
        }


        return 0;
    }

    public boolean isWaterSourceBlock(int x, int y, int z) {
        BaseBlockState bukkitBlock = getWrappedBlockStateAt(x, y, z);

        if (bukkitBlock instanceof MagicBlockState) {
            return ((MagicBlockState) bukkitBlock).getData() == 0;
        }

        if (bukkitBlock instanceof FlatBlockState && ((FlatBlockState) bukkitBlock).getBlockData() instanceof Levelled && bukkitBlock.getMaterial() == WATER) {
            return ((Levelled) ((FlatBlockState) bukkitBlock).getBlockData()).getLevel() == 0;
        }

        // These blocks are also considered source blocks

        return Materials.checkFlag(bukkitBlock.getMaterial(), player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_13) ? Materials.WATER_SOURCE : Materials.WATER_SOURCE_LEGACY);
    }

    public boolean containsLiquid(SimpleCollisionBox var0) {
        int var1 = (int) Math.floor(var0.minX);
        int var2 = (int) Math.ceil(var0.maxX);
        int var3 = (int) Math.floor(var0.minY);
        int var4 = (int) Math.ceil(var0.maxY);
        int var5 = (int) Math.floor(var0.minZ);
        int var6 = (int) Math.ceil(var0.maxZ);

        for (int var8 = var1; var8 < var2; ++var8) {
            for (int var9 = var3; var9 < var4; ++var9) {
                for (int var10 = var5; var10 < var6; ++var10) {
                    if (player.compensatedWorld.getFluidLevelAt(var8, var9, var10) > 0) return true;
                }
            }
        }

        return false;
    }

    public void removeChunk(int chunkX, int chunkZ) {
        long chunkPosition = chunkPositionToLong(chunkX, chunkZ);
        chunks.remove(chunkPosition);

        openShulkerBoxes.removeIf(data -> data.position.getX() >> 4 == chunkX && data.position.getZ() >> 4 == chunkZ);
    }
}
