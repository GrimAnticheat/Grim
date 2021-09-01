package ac.grim.grimac.utils.latency;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.WrappedBlockData;
import ac.grim.grimac.utils.blockdata.types.*;
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
import ac.grim.grimac.utils.data.*;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntityShulker;
import ac.grim.grimac.utils.data.packetentity.latency.BlockPlayerUpdate;
import ac.grim.grimac.utils.enums.EntityType;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.nmsImplementations.GetBoundingBox;
import ac.grim.grimac.utils.nmsImplementations.Materials;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.utils.pair.Pair;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3i;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;

// Inspired by https://github.com/GeyserMC/Geyser/blob/master/connector/src/main/java/org/geysermc/connector/network/session/cache/ChunkCache.java
public class CompensatedWorld {
    public static final int MIN_WORLD_HEIGHT = 0;
    public static final int MAX_WORLD_HEIGHT = 255;
    public static BaseBlockState airData;
    public static Method getByCombinedID;
    public final GrimPlayer player;
    private final Long2ObjectMap<Column> chunks = new Long2ObjectOpenHashMap<>();
    public ConcurrentSkipListSet<BasePlayerChangeBlockData> worldChangedBlockQueue = new ConcurrentSkipListSet<>((a, b) -> {
        // We can't have elements with equal comparisons, otherwise they won't be added
        if (a.transaction == b.transaction) {
            boolean aOpenBlock = a instanceof PlayerOpenBlockData;
            boolean bOpenBlock = b instanceof PlayerOpenBlockData;

            if (aOpenBlock != bOpenBlock) return Boolean.compare(aOpenBlock, bOpenBlock);

            return Integer.compare(a.hashCode(), b.hashCode());
        }
        return Integer.compare(a.transaction, b.transaction);
    });
    public ConcurrentLinkedQueue<Pair<Integer, Vector3i>> unloadChunkQueue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<PistonData> pistonData = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<BlockPlayerUpdate> packetBlockPlaces = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<BlockPlayerUpdate> packetBlockBreaks = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<TransPosData> packetBucket = new ConcurrentLinkedQueue<>();

    public ConcurrentLinkedQueue<Pair<Integer, Vector3i>> likelyDesyncBlockPositions = new ConcurrentLinkedQueue<>();

    // Packet locations for blocks
    public ConcurrentLinkedQueue<Pair<Integer, Vector3i>> packetLevelBlockLocations = new ConcurrentLinkedQueue<>();

    public List<PistonData> activePistons = new ArrayList<>();
    public Set<ShulkerData> openShulkerBoxes = ConcurrentHashMap.newKeySet();
    public boolean isResync = true;

    public CompensatedWorld(GrimPlayer player) {
        this.player = player;
    }

    public static void init() {
        if (XMaterial.isNewVersion()) {
            airData = new FlatBlockState(0);
        } else {
            airData = new MagicBlockState(0, 0);
        }
    }

    public boolean isNearHardEntity(SimpleCollisionBox playerBox) {
        for (PacketEntity entity : player.compensatedEntities.entityMap.values()) {
            if (entity.type == EntityType.BOAT || entity.type == EntityType.SHULKER) {
                SimpleCollisionBox box = GetBoundingBox.getBoatBoundingBox(entity.position.getX(), entity.position.getY(), entity.position.getZ());
                if (box.isIntersected(playerBox)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void tickUpdates(int lastTransactionReceived) {
        while (true) {
            Pair<Integer, Vector3i> data = unloadChunkQueue.peek();

            if (data == null) break;

            // The player hasn't gotten this update yet
            if (data.getFirst() > lastTransactionReceived) {
                break;
            }

            unloadChunkQueue.poll();

            int chunkX = data.getSecond().getX();
            int chunkZ = data.getSecond().getZ();

            long chunkPosition = chunkPositionToLong(chunkX, chunkZ);

            // Don't unload the chunk if this is a different chunk than what we actually wanted.
            Column loadedChunk = getChunk(chunkX, chunkZ);
            if (loadedChunk != null && loadedChunk.transaction < data.getFirst()) {
                chunks.remove(chunkPosition);
                openShulkerBoxes.removeIf(box -> box.position.getX() >> 4 == chunkX && box.position.getZ() >> 4 == chunkZ);
            }
        }

        for (Iterator<BasePlayerChangeBlockData> it = worldChangedBlockQueue.iterator(); it.hasNext(); ) {
            BasePlayerChangeBlockData changeBlockData = it.next();
            if (changeBlockData.transaction > lastTransactionReceived) {
                break;
            }

            it.remove();

            likelyDesyncBlockPositions.add(new Pair<>(player.lastTransactionSent.get(), new Vector3i(changeBlockData.blockX, changeBlockData.blockY, changeBlockData.blockZ)));

            if (changeBlockData instanceof PlayerOpenBlockData) {
                tickOpenable((PlayerOpenBlockData) changeBlockData);
                continue;
            }

            player.compensatedWorld.updateBlock(changeBlockData.blockX, changeBlockData.blockY, changeBlockData.blockZ, changeBlockData.getCombinedID());
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

        // 3 ticks is enough for everything that needs to be processed to be processed
        packetBlockPlaces.removeIf(data -> GrimAPI.INSTANCE.getTickManager().getTick() - data.tick > 3);
        packetBlockBreaks.removeIf(data -> GrimAPI.INSTANCE.getTickManager().getTick() - data.tick > 3);
        packetBucket.removeIf(data -> GrimAPI.INSTANCE.getTickManager().getTick() - data.getTick() > 3);
        likelyDesyncBlockPositions.removeIf(data -> player.packetStateData.packetLastTransactionReceived.get() > data.getFirst());

        packetLevelBlockLocations.removeIf(data -> GrimAPI.INSTANCE.getTickManager().getTick() - data.getFirst() > 3);
    }

    public boolean hasPacketBlockAt(SimpleCollisionBox box) {
        for (Pair<Integer, Vector3i> block : packetLevelBlockLocations) {
            Vector3i pos = block.getSecond();

            if (pos.getX() >= box.minX && pos.getX() <= box.maxX &&
                    pos.getY() >= box.minY && pos.getY() <= box.maxY &&
                    pos.getZ() >= box.minZ && pos.getZ() <= box.maxZ)
                return true;
        }

        return false;
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

    public void tickOpenable(PlayerOpenBlockData blockToOpen) {
        MagicBlockState data = (MagicBlockState) player.compensatedWorld.getWrappedBlockStateAt(blockToOpen.blockX, blockToOpen.blockY, blockToOpen.blockZ);
        WrappedBlockDataValue blockDataValue = WrappedBlockData.getMaterialData(data);

        if (blockDataValue instanceof WrappedDoor) {
            WrappedDoor door = (WrappedDoor) blockDataValue;
            MagicBlockState otherDoor = (MagicBlockState) player.compensatedWorld.getWrappedBlockStateAt(blockToOpen.blockX, blockToOpen.blockY + (door.isBottom() ? 1 : -1), blockToOpen.blockZ);

            // The doors seem connected (Remember this is 1.12- where doors are dependent on one another for data
            if (otherDoor.getMaterial() == data.getMaterial()) {
                // The doors are probably connected
                boolean isBottom = door.isBottom();
                // Add the other door part to the likely to desync positions
                player.compensatedWorld.likelyDesyncBlockPositions.add(new Pair<>(player.lastTransactionSent.get(), new Vector3i(blockToOpen.blockX, blockToOpen.blockY + (isBottom ? 1 : -1), blockToOpen.blockZ)));
                // 1.12- stores door data in the bottom door
                if (!isBottom)
                    data = otherDoor;
                // 1.13+ - We need to grab the bukkit block data, flip the open state, then get combined ID
                // 1.12- - We can just flip a bit in the lower door and call it a day
                int magicValue = data.getId() | ((data.getBlockData() ^ 0x4) << 12);
                player.compensatedWorld.updateBlock(blockToOpen.blockX, blockToOpen.blockY + (isBottom ? 0 : -1), blockToOpen.blockZ, magicValue);
            }
        } else if (blockDataValue instanceof WrappedTrapdoor || blockDataValue instanceof WrappedFenceGate) {
            // Take 12 most significant bytes -> the material ID.  Combine them with the new block magic data.
            int magicValue = data.getId() | ((data.getBlockData() ^ 0x4) << 12);
            player.compensatedWorld.updateBlock(blockToOpen.blockX, blockToOpen.blockY, blockToOpen.blockZ, magicValue);
        }
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

    public Column getChunk(int chunkX, int chunkZ) {
        long chunkPosition = chunkPositionToLong(chunkX, chunkZ);
        return chunks.get(chunkPosition);
    }

    public static long chunkPositionToLong(int x, int z) {
        return ((x & 0xFFFFFFFFL) << 32L) | (z & 0xFFFFFFFFL);
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

    public boolean isWaterSourceBlock(int x, int y, int z) {
        BaseBlockState bukkitBlock = getWrappedBlockStateAt(x, y, z);

        return ((MagicBlockState) bukkitBlock).getBlockData() == 0;
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

    public double getLavaFluidLevelAt(int x, int y, int z) {
        MagicBlockState magicBlockState = (MagicBlockState) getWrappedBlockStateAt(x, y, z);

        if (!Materials.checkFlag(magicBlockState.getMaterial(), Materials.LAVA)) return 0;

        // If it is lava or flowing lava
        if (magicBlockState.getId() == 10 || magicBlockState.getId() == 11) {
            int magicData = magicBlockState.getBlockData();

            // Falling lava has a level of 8
            if ((magicData & 0x8) == 8) return 8 / 9f;

            return (8 - magicData) / 9f;
        }

        return 0;
    }

    public boolean containsLava(SimpleCollisionBox var0) {
        int var1 = (int) Math.floor(var0.minX);
        int var2 = (int) Math.ceil(var0.maxX);
        int var3 = (int) Math.floor(var0.minY);
        int var4 = (int) Math.ceil(var0.maxY);
        int var5 = (int) Math.floor(var0.minZ);
        int var6 = (int) Math.ceil(var0.maxZ);

        for (int var8 = var1; var8 < var2; ++var8) {
            for (int var9 = var3; var9 < var4; ++var9) {
                for (int var10 = var5; var10 < var6; ++var10) {
                    if (player.compensatedWorld.getLavaFluidLevelAt(var8, var9, var10) > 0) return true;
                }
            }
        }

        return false;
    }

    public double getWaterFluidLevelAt(double x, double y, double z) {
        return getWaterFluidLevelAt(GrimMath.floor(x), GrimMath.floor(y), GrimMath.floor(z));
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
            int magicData = magicBlockState.getBlockData();

            // Falling water has a level of 8
            if ((magicData & 0x8) == 8) return 8 / 9f;

            return (8 - magicData) / 9f;
        }

        return 0;
    }

    public void removeChunkLater(int chunkX, int chunkZ) {
        long chunkPosition = chunkPositionToLong(chunkX, chunkZ);
        Column column = chunks.get(chunkPosition);

        if (column == null) return;

        // Signify that there could be a desync between this and netty
        column.markedForRemoval = true;
        unloadChunkQueue.add(new Pair<>(player.lastTransactionSent.get() + 1, new Vector3i(chunkX, 0, chunkZ)));
    }
}
