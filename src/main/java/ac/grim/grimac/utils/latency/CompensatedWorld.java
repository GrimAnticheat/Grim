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
import ac.grim.grimac.utils.data.BasePlayerChangeBlockData;
import ac.grim.grimac.utils.data.ChangeBlockData;
import ac.grim.grimac.utils.data.PistonData;
import ac.grim.grimac.utils.data.ShulkerData;
import ac.grim.grimac.utils.data.packetentity.PacketEntityShulker;
import ac.grim.grimac.utils.data.packetentity.latency.BlockPlayerUpdate;
import ac.grim.grimac.utils.nmsImplementations.Materials;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

// Inspired by https://github.com/GeyserMC/Geyser/blob/master/connector/src/main/java/org/geysermc/connector/network/session/cache/ChunkCache.java
public class CompensatedWorld {
    public static final int MIN_WORLD_HEIGHT = 0;
    public static final int MAX_WORLD_HEIGHT = 255;
    public static BaseBlockState airData;
    public static Method getByCombinedID;

    public static void init() {
        if (XMaterial.isNewVersion()) {
            airData = new FlatBlockState(0);
        } else {
            airData = new MagicBlockState(0, 0);
        }
    }

    public final GrimPlayer player;
    private final Long2ObjectMap<Column> chunks = new Long2ObjectOpenHashMap<>();
    public ConcurrentLinkedQueue<ChangeBlockData> worldChangedBlockQueue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<BasePlayerChangeBlockData> changeBlockQueue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<PistonData> pistonData = new ConcurrentLinkedQueue<>();

    public ConcurrentLinkedQueue<BlockPlayerUpdate> packetBlockPositions = new ConcurrentLinkedQueue<>();

    public List<PistonData> activePistons = new ArrayList<>();
    public Set<ShulkerData> openShulkerBoxes = ConcurrentHashMap.newKeySet();

    public CompensatedWorld(GrimPlayer player) {
        this.player = player;
    }


    public void tickPlayerUpdates(int lastTransactionReceived) {
        while (true) {
            BasePlayerChangeBlockData changeBlockData = changeBlockQueue.peek();

            if (changeBlockData == null) break;
            // The anticheat thread is behind, this event has not occurred yet
            if (changeBlockData.transaction > lastTransactionReceived) break;
            changeBlockQueue.poll();

            player.compensatedWorld.updateBlock(changeBlockData.blockX, changeBlockData.blockY, changeBlockData.blockZ, changeBlockData.getCombinedID());
        }
    }

    public void updateBlock(int x, int y, int z, int combinedID) {
        Column column = getChunk(x >> 4, z >> 4);

        try {
            if (column != null) {
                BaseChunk chunk = column.getChunks()[y >> 4];
                if (chunk == null) {
                    if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_16)) {
                        column.getChunks()[y >> 4] = new SixteenChunk();
                    } else if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_13)) {
                        column.getChunks()[y >> 4] = new FifteenChunk();
                    } else if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_9)) {
                        column.getChunks()[y >> 4] = new TwelveChunk();
                    } else if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_8)) {
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
            }
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

    public void tickUpdates(int lastTransactionReceived) {
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

        // 10 ticks is more than enough for everything that needs to be processed to be processed
        packetBlockPositions.removeIf(data -> GrimAC.getCurrentTick() - data.tick > 10);
    }

    public void tickPlayerInPistonPushingArea() {
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
                    modX = Math.abs(data.direction.getModX()) * 0.51D;
                    modY = Math.abs(data.direction.getModY()) * 0.51D;
                    modZ = Math.abs(data.direction.getModZ()) * 0.51D;

                    playerBox.expandMax(modX, modY, modZ);
                    playerBox.expandMin(modX * -1, modY * -1, modZ * -1);

                    if (data.hasSlimeBlock || (data.hasHoneyBlock && player.getClientVersion().isOlderThan(ClientVersion.v_1_15_2))) {
                        player.uncertaintyHandler.slimePistonBounces.add(data.direction);
                    }

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
                    data.position.getX() + 1, data.position.getY() + 1, data.position.getZ() + 1, true);

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

        // Reduce effects of piston pushing by 0.5 per tick
        player.uncertaintyHandler.pistonPushing.add(Math.max(Math.max(player.uncertaintyHandler.pistonX, player.uncertaintyHandler.pistonY), player.uncertaintyHandler.pistonZ) * (player.uncertaintyHandler.slimePistonBounces.isEmpty() ? 1 : 2));

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
        MagicBlockState bukkitBlock = (MagicBlockState) getWrappedBlockStateAt(x, y, z);

        return ((bukkitBlock.getBlockData() & 0x8) == 8);
    }

    public double getLavaFluidLevelAt(int x, int y, int z) {
        MagicBlockState magicBlockState = (MagicBlockState) getWrappedBlockStateAt(x, y, z);

        if (!Materials.checkFlag(magicBlockState.getMaterial(), Materials.LAVA)) return 0;

        // If it is lava or flowing lava
        if (magicBlockState.getId() == 10 || magicBlockState.getId() == 11) {
            int magicData = magicBlockState.getData();

            // Falling lava has a level of 8
            if ((magicData & 0x8) == 8) return 8 / 9f;

            return (8 - magicData) / 9f;
        }

        return 0;
    }

    public boolean isWaterSourceBlock(int x, int y, int z) {
        BaseBlockState bukkitBlock = getWrappedBlockStateAt(x, y, z);

        return ((MagicBlockState) bukkitBlock).getData() == 0;
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

    public boolean containsWater(SimpleCollisionBox var0) {
        int var1 = (int) Math.floor(var0.minX);
        int var2 = (int) Math.ceil(var0.maxX);
        int var3 = (int) Math.floor(var0.minY);
        int var4 = (int) Math.ceil(var0.maxY);
        int var5 = (int) Math.floor(var0.minZ);
        int var6 = (int) Math.ceil(var0.maxZ);

        for (int var8 = var1; var8 < var2; ++var8) {
            for (int var9 = var3; var9 < var4; ++var9) {
                for (int var10 = var5; var10 < var6; ++var10) {
                    if (player.compensatedWorld.getWaterFluidLevelAt(var8, var9, var10) > 0) return true;
                }
            }
        }

        return false;
    }

    public double getWaterFluidLevelAt(int x, int y, int z) {
        BaseBlockState bukkitBlock = getWrappedBlockStateAt(x, y, z);
        boolean isWater = Materials.isWaterMagic(player.getClientVersion(), bukkitBlock);

        if (!isWater) return 0;

        // If water has water above it, it's block height is 1, even if it's waterlogged
        if (Materials.isWaterMagic(player.getClientVersion(), getWrappedBlockStateAt(x, y + 1, z))) {
            return 1;
        }

        MagicBlockState magicBlockState = (MagicBlockState) bukkitBlock;

        // If it is water or flowing water
        if (magicBlockState.getId() == 8 || magicBlockState.getId() == 9) {
            int magicData = magicBlockState.getData();

            // Falling water has a level of 8
            if ((magicData & 0x8) == 8) return 8 / 9f;

            return (8 - magicData) / 9f;
        }

        return 0;
    }

    public void removeChunk(int chunkX, int chunkZ) {
        long chunkPosition = chunkPositionToLong(chunkX, chunkZ);
        chunks.remove(chunkPosition);

        openShulkerBoxes.removeIf(data -> data.position.getX() >> 4 == chunkX && data.position.getZ() >> 4 == chunkZ);
    }
}
