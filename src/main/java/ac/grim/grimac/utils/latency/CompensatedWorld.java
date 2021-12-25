package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
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
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.enums.*;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.protocol.world.states.type.StateValue;
import com.github.retrooper.packetevents.util.Vector3i;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Inspired by https://github.com/GeyserMC/Geyser/blob/master/connector/src/main/java/org/geysermc/connector/network/session/cache/ChunkCache.java
public class CompensatedWorld {
    private static WrappedBlockState airData = WrappedBlockState.getByGlobalId(0);
    public final GrimPlayer player;
    private final Map<Long, Column> chunks;
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

    public static long chunkPositionToLong(int x, int z) {
        return ((x & 0xFFFFFFFFL) << 32L) | (z & 0xFFFFFFFFL);
    }

    public boolean isNearHardEntity(SimpleCollisionBox playerBox) {
        for (PacketEntity entity : player.compensatedEntities.entityMap.values()) {
            if (entity.type == EntityTypes.BOAT || entity.type == EntityTypes.SHULKER) {
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
                    // TODO: Pre-1.18 support
                    if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_16)) {
                        column.getChunks()[y >> 4] = new Chunk_v1_18();
                    }

                    chunk = column.getChunks()[y >> 4];

                    // Sets entire chunk to air
                    // This glitch/feature occurs due to the palette size being 0 when we first create a chunk section
                    // Meaning that all blocks in the chunk will refer to palette #0, which we are setting to air
                    chunk.set(0, 0, 0, 0);
                }

                chunk.set(x & 0xF, y & 0xF, z & 0xF, combinedID);

                // Handle stupidity such as fluids changing in idle ticks.
                if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13)) {
                    player.pointThreeEstimator.handleChangeBlock(x, y, z, WrappedBlockState.getByGlobalId(combinedID));
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void tickOpenable(int blockX, int blockY, int blockZ) {
        WrappedBlockState data = player.compensatedWorld.getWrappedBlockStateAt(blockX, blockY, blockZ);

        if (BlockTags.DOORS.contains(data.getType())) {
            WrappedBlockState otherDoor = player.compensatedWorld.getWrappedBlockStateAt(blockX,
                    blockY + (data.getHalf() == Half.BOTTOM ? 1 : -1), blockZ);

            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13)) {
                if (BlockTags.DOORS.contains(otherDoor.getType())) {
                    otherDoor.setOpen(!otherDoor.isOpen());
                    player.compensatedWorld.updateBlock(blockX, blockY + (data.getHalf() == Half.BOTTOM ? 1 : -1), blockZ, otherDoor.getGlobalId());
                }
            } else {
                // The doors seem connected (Remember this is 1.12- where doors are dependent on one another for data
                if (otherDoor.getType() == data.getType()) {
                    // The doors are probably connected
                    boolean isBottom = data.isBottom();
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

                    if (data.hasSlimeBlock || (data.hasHoneyBlock && player.getClientVersion().isOlderThan(ClientVersion.V_1_15_2))) {
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
                WrappedBlockState state = player.compensatedWorld.getWrappedBlockStateAt(data.blockPos.getX(), data.blockPos.getY(), data.blockPos.getZ());
                direction = state.getFacing();
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

    public WrappedBlockState getWrappedBlockStateAt(Vector3i vector3i) {
        return getWrappedBlockStateAt(vector3i.getX(), vector3i.getY(), vector3i.getZ());
    }

    public WrappedBlockState getWrappedBlockStateAt(int x, int y, int z) {
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

    public double getFluidLevelAt(double x, double y, double z) {
        return getFluidLevelAt((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    public double getFluidLevelAt(int x, int y, int z) {
        return Math.max(getWaterFluidLevelAt(x, y, z), getLavaFluidLevelAt(x, y, z));
    }

    public boolean isFluidFalling(int x, int y, int z) {
        WrappedBlockState bukkitBlock = getWrappedBlockStateAt(x, y, z);
        return bukkitBlock.getLevel() >= 8;
    }

    public boolean isWaterSourceBlock(int x, int y, int z) {
        WrappedBlockState bukkitBlock = getWrappedBlockStateAt(x, y, z);
        return Materials.isWaterSource(player.getClientVersion(), bukkitBlock);
    }

    public boolean containsLiquid(SimpleCollisionBox var0) {
        return Collisions.hasMaterial(player, var0, data -> Materials.isWater(player.getClientVersion(), data) || data.getType() == StateTypes.LAVA;
    }

    public boolean containsWater(SimpleCollisionBox var0) {
        return Collisions.hasMaterial(player, var0, data -> Materials.isWater(player.getClientVersion(), data));
    }

    public double getLavaFluidLevelAt(int x, int y, int z) {
        WrappedBlockState magicBlockState = getWrappedBlockStateAt(x, y, z);
        WrappedBlockState magicBlockStateAbove = getWrappedBlockStateAt(x, y, z);

        if (magicBlockState.getType() != StateTypes.LAVA) return 0;
        if (magicBlockStateAbove.getType() == StateTypes.LAVA) return 1;

        int level = magicBlockState.getLevel();

        // If it is lava or flowing lava
        if (magicBlockState.getLevel() >= 8) {
            // Falling lava has a level of 8
            if (level >= 8) return 8 / 9f;

            return (8 - level) / 9f;
        }

        return 0;
    }

    public boolean containsLava(SimpleCollisionBox var0) {
        return Collisions.hasMaterial(player, var0, data -> data.getType() == StateTypes.LAVA);
    }

    public double getWaterFluidLevelAt(double x, double y, double z) {
        return getWaterFluidLevelAt(GrimMath.floor(x), GrimMath.floor(y), GrimMath.floor(z));
    }

    public double getWaterFluidLevelAt(int x, int y, int z) {
        WrappedBlockState bukkitBlock = getWrappedBlockStateAt(x, y, z);
        boolean isWater = Materials.isWater(player.getClientVersion(), bukkitBlock);

        if (!isWater) return 0;

        // If water has water above it, it's block height is 1, even if it's waterlogged
        if (Materials.isWater(player.getClientVersion(), getWrappedBlockStateAt(x, y + 1, z))) {
            return 1;
        }

        // If it is water or flowing water
        if (bukkitBlock.getType() == StateTypes.WATER) {
            int magicData = bukkitBlock.getLevel();

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

    public WrappedBlockState getWrappedBlockStateAt(Vector aboveCCWPos) {
        return getWrappedBlockStateAt(aboveCCWPos.getX(), aboveCCWPos.getY(), aboveCCWPos.getZ());
    }
}
