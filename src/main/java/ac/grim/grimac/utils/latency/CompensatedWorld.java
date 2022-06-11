package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.ClientBlockPrediction;
import ac.grim.grimac.utils.data.PistonData;
import ac.grim.grimac.utils.data.ShulkerData;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntityShulker;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.nmsutil.Collisions;
import ac.grim.grimac.utils.nmsutil.Materials;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v1_16.Chunk_v1_9;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.DataPalette;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.ListPalette;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.PaletteType;
import com.github.retrooper.packetevents.protocol.world.chunk.storage.LegacyFlexibleStorage;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.enums.*;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.protocol.world.states.type.StateValue;
import com.github.retrooper.packetevents.util.Vector3i;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Setter;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// Inspired by https://github.com/GeyserMC/Geyser/blob/master/connector/src/main/java/org/geysermc/connector/network/session/cache/ChunkCache.java
public class CompensatedWorld {
    public static final ClientVersion blockVersion = PacketEvents.getAPI().getServerManager().getVersion().toClientVersion();
    private static final WrappedBlockState airData = WrappedBlockState.getByGlobalId(blockVersion, 0);
    public final GrimPlayer player;
    public final Map<Long, Column> chunks;
    // Packet locations for blocks
    public Set<PistonData> activePistons = ConcurrentHashMap.newKeySet();
    public Set<ShulkerData> openShulkerBoxes = ConcurrentHashMap.newKeySet();
    // 1.17 with datapacks, and 1.18, have negative world offset values
    private int minHeight = 0;
    private int maxHeight = 256;

    public Long2ObjectOpenHashMap<ClientBlockPrediction> clientPredictions = new Long2ObjectOpenHashMap<>();
    @Setter
    boolean isPrediction;
    int lastReceivedSequence;

    public CompensatedWorld(GrimPlayer player) {
        this.player = player;
        chunks = new Long2ObjectOpenHashMap<>(81, 0.5f);
    }

    public static long chunkPositionToLong(int x, int z) {
        return ((x & 0xFFFFFFFFL) << 32L) | (z & 0xFFFFFFFFL);
    }

    public boolean isNearHardEntity(SimpleCollisionBox playerBox) {
        for (PacketEntity entity : player.compensatedEntities.entityMap.values()) {
            if ((entity.type == EntityTypes.BOAT || entity.type == EntityTypes.SHULKER) && player.compensatedEntities.getSelf().getRiding() != entity) {
                SimpleCollisionBox box = entity.getPossibleCollisionBoxes();
                if (box.isIntersected(playerBox)) {
                    return true;
                }
            }
        }

        // Also block entities
        for (ShulkerData data : openShulkerBoxes) {
            SimpleCollisionBox shulkerCollision = data.getCollision();
            if (playerBox.isCollided(shulkerCollision)) {
                return true;
            }
        }

        // Pistons are a block entity.
        for (PistonData data : activePistons) {
            for (SimpleCollisionBox box : data.boxes) {
                if (playerBox.isCollided(box)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static BaseChunk create() {
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_18)) {
            return new Chunk_v1_18();
        } else if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_16)) {
            return new Chunk_v1_9(0, DataPalette.createForChunk());
        }
        return new Chunk_v1_9(0, new DataPalette(new ListPalette(4), new LegacyFlexibleStorage(4, 4096), PaletteType.CHUNK));
    }

    public void incrementSequence() {
        lastReceivedSequence++;
    }

    public void updateBlock(int x, int y, int z, int combinedID) {
        long serializedPos = new Vector3i(x, y, z).getSerializedPosition();

        // Update the prediction and return
        if (!isPrediction && clientPredictions.containsKey(serializedPos)) {
            clientPredictions.get(serializedPos).setBlockId(combinedID);
            return;
        }

        Column column = getChunk(x >> 4, z >> 4);

        // Apply 1.17 expanded world offset
        int offsetY = y - minHeight;

        if (column != null) {
            if (column.getChunks().length <= (offsetY >> 4)) return;

            BaseChunk chunk = column.getChunks()[offsetY >> 4];

            if (chunk == null) {
                chunk = create();
                column.getChunks()[offsetY >> 4] = chunk;

                // Sets entire chunk to air
                // This glitch/feature occurs due to the palette size being 0 when we first create a chunk section
                // Meaning that all blocks in the chunk will refer to palette #0, which we are setting to air
                chunk.set(null, 0, 0, 0, 0);
            }

            // Store previous state when placing blocks etc.
            if (isPrediction) {
                clientPredictions.put(serializedPos, new ClientBlockPrediction(lastReceivedSequence, chunk.get(blockVersion, x & 0xF, offsetY & 0xF, z & 0xF).getGlobalId(), new Vector3i(x, y, z)));
            }

            chunk.set(null, x & 0xF, offsetY & 0xF, z & 0xF, combinedID);

            // Handle stupidity such as fluids changing in idle ticks.
            player.pointThreeEstimator.handleChangeBlock(x, y, z, WrappedBlockState.getByGlobalId(blockVersion, combinedID));
        }
    }

    public void tickOpenable(int blockX, int blockY, int blockZ) {
        WrappedBlockState data = player.compensatedWorld.getWrappedBlockStateAt(blockX, blockY, blockZ);

        if (BlockTags.DOORS.contains(data.getType()) && data.getType() != StateTypes.IRON_DOOR) {
            WrappedBlockState otherDoor = player.compensatedWorld.getWrappedBlockStateAt(blockX,
                    blockY + (data.getHalf() == Half.LOWER ? 1 : -1), blockZ);

            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13)) {
                if (BlockTags.DOORS.contains(otherDoor.getType())) {
                    otherDoor.setOpen(!otherDoor.isOpen());
                    player.compensatedWorld.updateBlock(blockX, blockY + (data.getHalf() == Half.LOWER ? 1 : -1), blockZ, otherDoor.getGlobalId());
                }
                data.setOpen(!data.isOpen());
                player.compensatedWorld.updateBlock(blockX, blockY, blockZ, data.getGlobalId());
            } else {
                // The doors seem connected (Remember this is 1.12- where doors are dependent on one another for data
                if (otherDoor.getType() == data.getType()) {
                    // The doors are probably connected
                    boolean isBottom = data.getHalf() == Half.LOWER;
                    // 1.12- stores door data in the bottom door
                    if (!isBottom)
                        data = otherDoor;
                    // 1.13+ - We need to grab the bukkit block data, flip the open state, then get combined ID
                    // 1.12- - We can just flip a bit in the lower door and call it a day
                    data.setOpen(!data.isOpen());
                    player.compensatedWorld.updateBlock(blockX, blockY + (isBottom ? 0 : -1), blockZ, data.getGlobalId());
                }
            }
        } else if (BlockTags.TRAPDOORS.contains(data.getType()) || BlockTags.FENCE_GATES.contains(data.getType())) {
            // Take 12 most significant bytes -> the material ID.  Combine them with the new block magic data.
            data.setOpen(!data.isOpen());
            player.compensatedWorld.updateBlock(blockX, blockY, blockZ, data.getGlobalId());
        }
    }

    public void tickPlayerInPistonPushingArea() {
        player.uncertaintyHandler.tick();
        // Occurs on player login
        if (player.boundingBox == null) return;
        SimpleCollisionBox playerBox = player.boundingBox.copy();

        double modX = 0;
        double modY = 0;
        double modZ = 0;

        for (PistonData data : activePistons) {
            for (SimpleCollisionBox box : data.boxes) {
                if (playerBox.isCollided(box)) {
                    modX = Math.max(modX, Math.abs(data.direction.getModX() * 0.51D));
                    modY = Math.max(modY, Math.abs(data.direction.getModY() * 0.51D));
                    modZ = Math.max(modZ, Math.abs(data.direction.getModZ() * 0.51D));

                    playerBox.expandMax(modX, modY, modZ);
                    playerBox.expandMin(modX * -1, modY * -1, modZ * -1);

                    if (data.hasSlimeBlock || (data.hasHoneyBlock && player.getClientVersion().isOlderThan(ClientVersion.V_1_15_2))) {
                        player.uncertaintyHandler.slimePistonBounces.add(data.direction);
                    }

                    break;
                }
            }
        }

        for (ShulkerData data : openShulkerBoxes) {
            SimpleCollisionBox shulkerCollision = data.getCollision();

            BlockFace direction;
            if (data.entity == null) {
                WrappedBlockState state = player.compensatedWorld.getWrappedBlockStateAt(data.blockPos.getX(), data.blockPos.getY(), data.blockPos.getZ());
                direction = state.getFacing();
            } else {
                direction = ((PacketEntityShulker) data.entity).facing.getOppositeFace();
            }

            if (direction == null) direction = BlockFace.UP; // default state

            // Change negative corner in expansion as the direction is negative
            // We don't bother differentiating shulker entities and shulker boxes
            // I guess players can cheat to get an extra 0.49 of Y height on shulker boxes, I don't care.
            if (direction.getModX() == -1 || direction.getModY() == -1 || direction.getModZ() == -1) {
                shulkerCollision.expandMin(direction.getModX(), direction.getModY(), direction.getModZ());
            } else {
                shulkerCollision.expandMax(direction.getModZ(), direction.getModY(), direction.getModZ());
            }

            if (playerBox.isCollided(shulkerCollision)) {
                modX = Math.max(modX, Math.abs(direction.getModX() * 0.51D));
                modY = Math.max(modY, Math.abs(direction.getModY() * 0.51D));
                modZ = Math.max(modZ, Math.abs(direction.getModZ() * 0.51D));

                playerBox.expandMax(modX, modY, modZ);
                playerBox.expandMin(modX, modY, modZ);
            }
        }

        player.uncertaintyHandler.pistonX.add(modX);
        player.uncertaintyHandler.pistonY.add(modY);
        player.uncertaintyHandler.pistonZ.add(modZ);

        // Tick the pistons and remove them if they can no longer exist
        activePistons.removeIf(PistonData::tickIfGuaranteedFinished);
        openShulkerBoxes.removeIf(ShulkerData::tickIfGuaranteedFinished);
        // Remove if a shulker is not in this block position anymore
        openShulkerBoxes.removeIf(box -> {
            if (box.blockPos != null) { // Block is no longer valid
                return !Materials.isShulker(player.compensatedWorld.getWrappedBlockStateAt(box.blockPos).getType());
            } else { // Entity is no longer valid
                return !player.compensatedEntities.entityMap.containsValue(box.entity);
            }
        });
    }

    public WrappedBlockState getWrappedBlockStateAt(Vector3i vector3i) {
        return getWrappedBlockStateAt(vector3i.getX(), vector3i.getY(), vector3i.getZ());
    }

    public WrappedBlockState getWrappedBlockStateAt(int x, int y, int z) {
        try {
            Column column = getChunk(x >> 4, z >> 4);

            y -= minHeight;
            if (column == null || y < 0 || (y >> 4) >= column.getChunks().length) return airData;

            BaseChunk chunk = column.getChunks()[y >> 4];
            if (chunk != null) {
                return chunk.get(blockVersion, x & 0xF, y & 0xF, z & 0xF);
            }
        } catch (Exception ignored) {
        }

        return airData;
    }

    // Not direct power into a block
    // Trapped chests give power but there's no packet to the client to actually apply this... ignore trapped chests
    // just like mojang did!
    public int getRawPowerAtState(BlockFace face, int x, int y, int z) {
        WrappedBlockState state = getWrappedBlockStateAt(x, y, z);

        if (state.getType() == StateTypes.REDSTONE_BLOCK) {
            return 15;
        } else if (state.getType() == StateTypes.DETECTOR_RAIL) { // Rails have directional requirement
            return state.isPowered() ? 15 : 0;
        } else if (state.getType() == StateTypes.REDSTONE_TORCH) {
            return face != BlockFace.UP && state.isLit() ? 15 : 0;
        } else if (state.getType() == StateTypes.REDSTONE_WIRE) {
            BlockFace needed = face.getOppositeFace();

            BlockFace badOne = needed.getCW();
            BlockFace badTwo = needed.getCCW();

            boolean isPowered = false;
            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13)) {
                switch (needed) {
                    case DOWN:
                        isPowered = true;
                        break;
                    case UP:
                        isPowered = state.isUp();
                        break;
                    case NORTH:
                        isPowered = state.getNorth() == North.TRUE;
                        if (isPowered && (badOne == BlockFace.NORTH || badTwo == BlockFace.NORTH)) {
                            return 0;
                        }
                        break;
                    case SOUTH:
                        isPowered = state.getSouth() == South.TRUE;
                        if (isPowered && (badOne == BlockFace.SOUTH || badTwo == BlockFace.SOUTH)) {
                            return 0;
                        }
                        break;
                    case WEST:
                        isPowered = state.getWest() == West.TRUE;
                        if (isPowered && (badOne == BlockFace.WEST || badTwo == BlockFace.WEST)) {
                            return 0;
                        }
                        break;
                    case EAST:
                        isPowered = state.getEast() == East.TRUE;
                        if (isPowered && (badOne == BlockFace.EAST || badTwo == BlockFace.EAST)) {
                            return 0;
                        }
                        break;
                }
            } else {
                isPowered = true; // whatever, just go off the block's power to see if it connects
            }

            return isPowered ? state.getPower() : 0;
        } else if (state.getType() == StateTypes.REDSTONE_WALL_TORCH) {
            return state.getFacing() != face && state.isPowered() ? 15 : 0;
        } else if (state.getType() == StateTypes.DAYLIGHT_DETECTOR) {
            return state.getPower();
        } else if (state.getType() == StateTypes.OBSERVER) {
            return state.getFacing() == face && state.isPowered() ? 15 : 0;
        } else if (state.getType() == StateTypes.REPEATER) {
            return state.getFacing() == face && state.isPowered() ? state.getPower() : 0;
        } else if (state.getType() == StateTypes.LECTERN) {
            return state.isPowered() ? 15 : 0;
        } else if (state.getType() == StateTypes.TARGET) {
            return state.getPower();
        }

        return 0;
    }

    // Redstone can power blocks indirectly by directly powering a block next to the block to power
    public int getDirectSignalAtState(BlockFace face, int x, int y, int z) {
        WrappedBlockState state = getWrappedBlockStateAt(x, y, z);

        if (state.getType() == StateTypes.DETECTOR_RAIL) { // Rails hard power block below itself
            boolean isPowered = (boolean) state.getInternalData().getOrDefault(StateValue.POWERED, false);
            return face == BlockFace.UP && isPowered ? 15 : 0;
        } else if (state.getType() == StateTypes.REDSTONE_TORCH) {
            return face != BlockFace.UP && state.isLit() ? 15 : 0;
        } else if (state.getType() == StateTypes.LEVER || BlockTags.BUTTONS.contains(state.getType())) {
            return state.getFacing().getOppositeFace() == face && state.isPowered() ? 15 : 0;
        } else if (state.getType() == StateTypes.REDSTONE_WALL_TORCH) {
            return face == BlockFace.DOWN && state.isPowered() ? 15 : 0;
        } else if (state.getType() == StateTypes.LECTERN) {
            return face == BlockFace.UP && state.isPowered() ? 15 : 0;
        } else if (state.getType() == StateTypes.OBSERVER) {
            return state.getFacing() == face && state.isPowered() ? 15 : 0;
        } else if (state.getType() == StateTypes.REPEATER) {
            return state.getFacing() == face && state.isPowered() ? state.getPower() : 0;
        } else if (state.getType() == StateTypes.REDSTONE_WIRE) {
            BlockFace needed = face.getOppositeFace();

            BlockFace badOne = needed.getCW();
            BlockFace badTwo = needed.getCCW();

            boolean isPowered = false;
            switch (needed) {
                case DOWN:
                case UP:
                    break;
                case NORTH:
                    isPowered = state.getNorth() == North.TRUE;
                    if (isPowered && (badOne == BlockFace.NORTH || badTwo == BlockFace.NORTH)) {
                        return 0;
                    }
                    break;
                case SOUTH:
                    isPowered = state.getSouth() == South.TRUE;
                    if (isPowered && (badOne == BlockFace.SOUTH || badTwo == BlockFace.SOUTH)) {
                        return 0;
                    }
                    break;
                case WEST:
                    isPowered = state.getWest() == West.TRUE;
                    if (isPowered && (badOne == BlockFace.WEST || badTwo == BlockFace.WEST)) {
                        return 0;
                    }
                    break;
                case EAST:
                    isPowered = state.getEast() == East.TRUE;
                    if (isPowered && (badOne == BlockFace.EAST || badTwo == BlockFace.EAST)) {
                        return 0;
                    }
                    break;
            }

            return isPowered ? state.getPower() : 0;
        }

        return 0;
    }

    public Column getChunk(int chunkX, int chunkZ) {
        long chunkPosition = chunkPositionToLong(chunkX, chunkZ);
        return chunks.get(chunkPosition);
    }

    public boolean isChunkLoaded(int chunkX, int chunkZ) {
        long chunkPosition = chunkPositionToLong(chunkX, chunkZ);
        return chunks.containsKey(chunkPosition);
    }

    public void addToCache(Column chunk, int chunkX, int chunkZ) {
        long chunkPosition = chunkPositionToLong(chunkX, chunkZ);
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> chunks.put(chunkPosition, chunk));
    }

    public StateType getStateTypeAt(double x, double y, double z) {
        return getWrappedBlockStateAt((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)).getType();
    }

    public WrappedBlockState getWrappedBlockStateAt(double x, double y, double z) {
        return getWrappedBlockStateAt((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    public double getFluidLevelAt(int x, int y, int z) {
        return Math.max(getWaterFluidLevelAt(x, y, z), getLavaFluidLevelAt(x, y, z));
    }

    public boolean isWaterSourceBlock(int x, int y, int z) {
        WrappedBlockState bukkitBlock = getWrappedBlockStateAt(x, y, z);
        return Materials.isWaterSource(player.getClientVersion(), bukkitBlock);
    }

    public boolean containsLiquid(SimpleCollisionBox var0) {
        return Collisions.hasMaterial(player, var0, data -> Materials.isWater(player.getClientVersion(), data.getFirst()) || data.getFirst().getType() == StateTypes.LAVA);
    }

    public double getLavaFluidLevelAt(int x, int y, int z) {
        WrappedBlockState magicBlockState = getWrappedBlockStateAt(x, y, z);
        WrappedBlockState magicBlockStateAbove = getWrappedBlockStateAt(x, y + 1, z);

        if (magicBlockState.getType() != StateTypes.LAVA) return 0;
        if (magicBlockStateAbove.getType() == StateTypes.LAVA) return 1;

        int level = magicBlockState.getLevel();

        // If it is lava or flowing lava
        if (level >= 8) {
            // Falling lava has a level of 8
            return 8 / 9f;
        }

        return (8 - level) / 9f;
    }

    public boolean containsLava(SimpleCollisionBox var0) {
        return Collisions.hasMaterial(player, var0, data -> data.getFirst().getType() == StateTypes.LAVA);
    }

    public double getWaterFluidLevelAt(double x, double y, double z) {
        return getWaterFluidLevelAt(GrimMath.floor(x), GrimMath.floor(y), GrimMath.floor(z));
    }

    public double getWaterFluidLevelAt(int x, int y, int z) {
        WrappedBlockState wrappedBlock = getWrappedBlockStateAt(x, y, z);
        boolean isWater = Materials.isWater(player.getClientVersion(), wrappedBlock);

        if (!isWater) return 0;

        // If water has water above it, it's block height is 1, even if it's waterlogged
        if (Materials.isWater(player.getClientVersion(), getWrappedBlockStateAt(x, y + 1, z))) {
            return 1;
        }

        // If it is water or flowing water
        if (wrappedBlock.getType() == StateTypes.WATER) {
            int level = wrappedBlock.getLevel();

            // Falling water has a level of 8
            if ((level & 0x8) == 8) return 8 / 9f;

            return (8 - level) / 9f;
        }

        // The block is water, isn't water material directly, and doesn't have block above, so it is waterlogged
        // or another source-like block such as kelp.
        return 8 / 9F;
    }

    public void removeChunkLater(int chunkX, int chunkZ) {
        long chunkPosition = chunkPositionToLong(chunkX, chunkZ);
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> player.compensatedWorld.chunks.remove(chunkPosition));
    }

    public int getMinHeight() {
        return minHeight;
    }

    public void setDimension(String dimension, User user) {
        // No world height NBT
        if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_17)) return;

        NBTCompound dimensionNBT = user.getWorldNBT(dimension).getCompoundTagOrNull("element");
        minHeight = dimensionNBT.getNumberTagOrThrow("min_y").getAsInt();
        maxHeight = minHeight + dimensionNBT.getNumberTagOrThrow("height").getAsInt();
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public WrappedBlockState getWrappedBlockStateAt(Vector aboveCCWPos) {
        return getWrappedBlockStateAt(aboveCCWPos.getX(), aboveCCWPos.getY(), aboveCCWPos.getZ());
    }
}
