package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.WrappedBlockData;
import ac.grim.grimac.utils.blockdata.types.*;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.blockstate.FlatBlockState;
import ac.grim.grimac.utils.blockstate.MagicBlockState;
import ac.grim.grimac.utils.chunkdata.BaseChunk;
import ac.grim.grimac.utils.chunkdata.fifteen.FifteenChunk;
import ac.grim.grimac.utils.chunkdata.seven.SevenChunk;
import ac.grim.grimac.utils.chunkdata.sixteen.SixteenChunk;
import ac.grim.grimac.utils.chunkdata.twelve.TwelveChunk;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.PistonData;
import ac.grim.grimac.utils.data.ShulkerData;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntityShulker;
import ac.grim.grimac.utils.enums.EntityType;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.nmsutil.Collisions;
import ac.grim.grimac.utils.nmsutil.Materials;
import ac.grim.grimac.utils.nmsutil.XMaterial;
import io.github.retrooper.packetevents.utils.pair.Pair;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3i;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.type.Lectern;
import org.bukkit.block.data.type.LightningRod;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

// Inspired by https://github.com/GeyserMC/Geyser/blob/master/connector/src/main/java/org/geysermc/connector/network/session/cache/ChunkCache.java
public class CompensatedWorld {
    public static BaseBlockState airData;
    public static Method getByCombinedID;
    public final GrimPlayer player;
    private final Map<Long, Column> chunks;
    public Queue<Pair<Integer, Vector3i>> likelyDesyncBlockPositions = new ConcurrentLinkedQueue<>();
    // Packet locations for blocks
    public List<PistonData> activePistons = new ArrayList<>();
    public Set<ShulkerData> openShulkerBoxes = ConcurrentHashMap.newKeySet();
    // 1.17 with datapacks, and 1.18, have negative world offset values
    private int minHeight = 0;
    private int maxHeight = 255;

    public CompensatedWorld(GrimPlayer player) {
        this.player = player;
        chunks = new Long2ObjectOpenHashMap<>(81, 0.5f);
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
                SimpleCollisionBox box = entity.getPossibleCollisionBoxes();
                if (box.isIntersected(playerBox)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void updateBlock(int x, int y, int z, int combinedID) {
        Column column = getChunk(x >> 4, z >> 4);

        // Apply 1.17 expanded world offset
        y -= minHeight;

        try {
            if (column != null) {
                BaseChunk chunk = column.getChunks()[y >> 4];

                if (chunk == null) {
                    if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_16)) {
                        column.getChunks()[y >> 4] = new SixteenChunk();
                    } else if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_13)) {
                        column.getChunks()[y >> 4] = new FifteenChunk();
                    } else if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_8)) {
                        column.getChunks()[y >> 4] = new TwelveChunk();
                    } else {
                        column.getChunks()[y >> 4] = new SevenChunk();
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

    public void tickOpenable(int blockX, int blockY, int blockZ) {
        MagicBlockState data = (MagicBlockState) player.compensatedWorld.getWrappedBlockStateAt(blockX, blockY, blockZ);
        WrappedBlockDataValue blockDataValue = WrappedBlockData.getMaterialData(data);

        if (blockDataValue instanceof WrappedDoor) {
            WrappedDoor door = (WrappedDoor) blockDataValue;
            MagicBlockState otherDoor = (MagicBlockState) player.compensatedWorld.getWrappedBlockStateAt(blockX, blockY + (door.isBottom() ? 1 : -1), blockZ);

            // The doors seem connected (Remember this is 1.12- where doors are dependent on one another for data
            if (otherDoor.getMaterial() == data.getMaterial()) {
                // The doors are probably connected
                boolean isBottom = door.isBottom();
                // Add the other door part to the likely to desync
                player.compensatedWorld.likelyDesyncBlockPositions.add(new Pair<>(player.lastTransactionSent.get(), new Vector3i(blockX, blockY + (isBottom ? 1 : -1), blockZ)));
                // 1.12- stores door data in the bottom door
                if (!isBottom)
                    data = otherDoor;
                // 1.13+ - We need to grab the bukkit block data, flip the open state, then get combined ID
                // 1.12- - We can just flip a bit in the lower door and call it a day
                int magicValue = data.getId() | ((data.getBlockData() ^ 0x4) << 12);
                player.compensatedWorld.updateBlock(blockX, blockY + (isBottom ? 0 : -1), blockZ, magicValue);
            }
        } else if (blockDataValue instanceof WrappedTrapdoor || blockDataValue instanceof WrappedFenceGate) {
            // Take 12 most significant bytes -> the material ID.  Combine them with the new block magic data.
            int magicValue = data.getId() | ((data.getBlockData() ^ 0x4) << 12);
            player.compensatedWorld.updateBlock(blockX, blockY, blockZ, magicValue);
        }
    }

    public void tickPlayerInPistonPushingArea() {
        player.uncertaintyHandler.tick();
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

            SimpleCollisionBox shulkerCollision = data.getCollision();

            BlockFace direction;
            if (data.entity == null) {
                BaseBlockState state = player.compensatedWorld.getWrappedBlockStateAt(data.blockPos.getX(), data.blockPos.getY(), data.blockPos.getZ());
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

    public BaseBlockState getWrappedBlockStateAt(Vector3i vector3i) {
        return getWrappedBlockStateAt(vector3i.getX(), vector3i.getY(), vector3i.getZ());
    }

    public BaseBlockState getWrappedBlockStateAt(int x, int y, int z) {
        Column column = getChunk(x >> 4, z >> 4);
        if (column == null || y < minHeight || y > maxHeight) return airData;

        y -= minHeight;

        BaseChunk chunk = column.getChunks()[y >> 4];
        if (chunk != null) {
            return chunk.get(x & 0xF, y & 0xF, z & 0xF);
        }

        return airData;
    }

    // Not direct power into a block
    // Trapped chests give power but there's no packet to the client to actually apply this... ignore trapped chests
    // just like mojang did!
    //
    // What anticheat codes in redstone logic?
    // Grim does to fix an issue where a player places doors/trapdoors on powered blocks!
    public int getRawPowerAtState(BlockFace face, int x, int y, int z) {
        BaseBlockState data = getWrappedBlockStateAt(x, y, z);
        WrappedBlockDataValue state = WrappedBlockData.getMaterialData(data).getData(data);

        if (data.getMaterial() == Material.REDSTONE_BLOCK) {
            return 15;
        } else if (state instanceof WrappedRails) { // Rails have directional requirement
            return face == BlockFace.UP ? ((WrappedRails) state).getPower() : 0;
        } else if (state instanceof WrappedRedstoneTorch) {
            return face != BlockFace.UP ? ((WrappedRedstoneTorch) state).getPower() : 0;
        } else if (state instanceof WrappedMultipleFacingPower) {
            return ((WrappedMultipleFacingPower) state).getDirections().contains(face.getOppositeFace()) ? ((WrappedMultipleFacingPower) state).getPower() : 0;
        } else if (state instanceof WrappedPower) {
            return ((WrappedPower) state).getPower();
        } else if (state instanceof WrappedWallTorchDirectionalPower) {
            return ((WrappedDirectionalPower) state).getDirection() != face && ((WrappedDirectionalPower) state).isPowered() ? 15 : 0;
        } else if (state instanceof WrappedDirectionalPower) {
            return ((WrappedDirectionalPower) state).isPowered() ? 15 : 0;
        } else if (state instanceof WrappedFlatBlock) {
            BlockData modernData = ((WrappedFlatBlock) state).getBlockData();

            // handles lectern
            if (modernData instanceof Powerable) {
                return ((Powerable) modernData).isPowered() ? 15 : 0;
            }
        }

        return 0;
    }

    // Redstone can power blocks indirectly by directly powering a block next to the block to power
    public int getDirectSignalAtState(BlockFace face, int x, int y, int z) {
        BaseBlockState data = getWrappedBlockStateAt(x, y, z);
        WrappedBlockDataValue state = WrappedBlockData.getMaterialData(data).getData(data);

        if (state instanceof WrappedRails) { // Rails have directional requirement
            return face == BlockFace.UP ? ((WrappedRails) state).getPower() : 0;
        } else if (state instanceof WrappedRedstoneTorch) {
            return face == BlockFace.DOWN ? ((WrappedRedstoneTorch) state).getPower() : 0;
        } else if (state instanceof WrappedMultipleFacingPower) {
            return ((WrappedMultipleFacingPower) state).getDirections().contains(face) ? ((WrappedMultipleFacingPower) state).getPower() : 0;
        } else if (state instanceof WrappedPower && data.getMaterial().name().contains("PLATE")) {
            return face == BlockFace.UP ? ((WrappedPower) state).getPower() : 0;
        } else if (state instanceof WrappedPower) {
            return ((WrappedPower) state).getPower();
        } else if (state instanceof WrappedWallTorchDirectionalPower) {
            return ((WrappedDirectionalPower) state).getDirection() != face && ((WrappedDirectionalPower) state).isPowered() ? 15 : 0;
        } else if (state instanceof WrappedDirectionalPower) {
            return ((WrappedDirectionalPower) state).getDirection() == face && ((WrappedDirectionalPower) state).isPowered() ? 15 : 0;
        } else if (state instanceof WrappedFlatBlock) {
            BlockData modernData = ((WrappedFlatBlock) state).getBlockData();

            // handles lectern
            if (modernData instanceof Lectern) {
                return face == BlockFace.UP && ((Lectern) modernData).isPowered() ? 15 : 0;
            } else if (modernData instanceof LightningRod) {
                return face == ((LightningRod) modernData).getFacing() && ((LightningRod) modernData).isPowered() ? 15 : 0;
            }
        }

        return 0;
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
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> chunks.put(chunkPosition, chunk));
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
        if (!Materials.isWater(player.getClientVersion(), bukkitBlock)) return false;
        // This is water, what is its fluid level?
        return ((MagicBlockState) bukkitBlock).getBlockData() == 0;
    }

    public boolean containsLiquid(SimpleCollisionBox var0) {
        return Collisions.hasMaterial(player, var0, data -> Materials.isWater(player.getClientVersion(), data) || Materials.checkFlag(data.getMaterial(), Materials.LAVA));
    }

    public boolean containsWater(SimpleCollisionBox var0) {
        return Collisions.hasMaterial(player, var0, data -> Materials.isWater(player.getClientVersion(), data));
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
        return Collisions.hasMaterial(player, var0, data -> Materials.checkFlag(data.getMaterial(), Materials.LAVA));
    }

    public double getWaterFluidLevelAt(double x, double y, double z) {
        return getWaterFluidLevelAt(GrimMath.floor(x), GrimMath.floor(y), GrimMath.floor(z));
    }

    public double getWaterFluidLevelAt(int x, int y, int z) {
        BaseBlockState bukkitBlock = getWrappedBlockStateAt(x, y, z);
        boolean isWater = Materials.isWaterIgnoringWaterlogged(player.getClientVersion(), bukkitBlock);

        if (!isWater) return 0;

        // If water has water above it, it's block height is 1, even if it's waterlogged
        if (Materials.isWaterIgnoringWaterlogged(player.getClientVersion(), getWrappedBlockStateAt(x, y + 1, z))) {
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
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> player.compensatedWorld.chunks.remove(chunkPosition));
    }

    public void setMaxWorldHeight(int maxSectionHeight) {
        this.maxHeight = maxSectionHeight;
    }

    public int getMinHeight() {
        return minHeight;
    }

    public void setMinHeight(int minHeight) {
        this.minHeight = minHeight;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public BaseBlockState getWrappedBlockStateAt(Vector aboveCCWPos) {
        return getWrappedBlockStateAt(aboveCCWPos.getX(), aboveCCWPos.getY(), aboveCCWPos.getZ());
    }
}
