package ac.grim.grimac.predictionengine.blockeffects;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.enums.PotentSulfurState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CompensatedGeysers {

    private static final ClientVersion SERVER_VERSION = PacketEvents.getAPI().getServerManager().getVersion().toClientVersion();

    private final Long2ObjectMap<Long2ObjectMap<GeyserBlockEntity>> tickersBySection = new Long2ObjectOpenHashMap<>();
    private long tickOrder = 0;

    public void addChunkToCache(Column chunk, TileEntity[] tileEntities, int minHeight) {
        if (SERVER_VERSION.isOlderThan(ClientVersion.V_26_2)) {
            return;
        }

        removeChunk(chunk.x(), chunk.z(), chunk.chunks().length);

        if (tileEntities != null) {
            addPotentSulfurTickersFromTileEntities(chunk.x(), chunk.z(), tileEntities, minHeight, chunk.chunks());
        }
    }

    public void updateBlock(int x, int y, int z, WrappedBlockState previousState, WrappedBlockState newState, int minHeight) {
        if (SERVER_VERSION.isOlderThan(ClientVersion.V_26_2)) {
            return;
        }

        boolean hadTicker = hasPotentSulfurTicker(previousState);
        boolean hasTicker = hasPotentSulfurTicker(newState);

        if (hadTicker && !hasTicker) {
            removeTicker(x, y, z, minHeight);
        } else if (!hadTicker && hasTicker) {
            addTicker(x, y, z, minHeight);
        }
    }

    public List<GeyserBlockEntity> getTickersInOrder(GrimPlayer player, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (SERVER_VERSION.isOlderThan(ClientVersion.V_26_2)) {
            return null;
        }

        List<GeyserBlockEntity> candidates = null;
        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;
        int minHeight = player.compensatedWorld.getMinHeight();

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                Column column = player.compensatedWorld.getChunk(chunkX, chunkZ);
                if (column == null) {
                    continue;
                }

                int minSection = Math.max(0, (minY - minHeight) >> 4);
                int maxSection = Math.min(column.chunks().length - 1, (maxY - minHeight) >> 4);
                for (int sectionIndex = minSection; sectionIndex <= maxSection; sectionIndex++) {
                    Long2ObjectMap<GeyserBlockEntity> section = this.tickersBySection.get(sectionKey(chunkX, chunkZ, sectionIndex));
                    if (section == null) {
                        continue;
                    }

                    for (GeyserBlockEntity geyser : section.values()) {
                        if (geyser.isInside(minX, minY, minZ, maxX, maxY, maxZ)) {
                            if (candidates == null) candidates = new ArrayList<>();
                            candidates.add(geyser);
                        }
                    }
                }
            }
        }

        if (candidates != null) {
            candidates.sort(Comparator.comparingLong(GeyserBlockEntity::tickOrder));
        }

        return candidates;
    }

    public void removeChunk(int chunkX, int chunkZ, int sectionCount) {
        for (int sectionIndex = 0; sectionIndex < sectionCount; sectionIndex++) {
            removeSection(chunkX, chunkZ, sectionIndex);
        }
    }

    public void clear() {
        this.tickersBySection.clear();
        this.tickOrder = 0;
    }

    private void addPotentSulfurTickersFromTileEntities(int chunkX, int chunkZ, TileEntity[] tileEntities, int minHeight, BaseChunk[] sections) {
        for (TileEntity tileEntity : tileEntities) {
            int x = (chunkX << 4) + tileEntity.getX();
            int y = tileEntity.getY();
            int z = (chunkZ << 4) + tileEntity.getZ();
            int sectionIndex = (y - minHeight) >> 4;

            if (sectionIndex < 0 || sectionIndex >= sections.length || sections[sectionIndex] == null) {
                continue;
            }

            WrappedBlockState state = sections[sectionIndex].get(SERVER_VERSION, x & 0xF, (y - minHeight) & 0xF, z & 0xF);
            if (hasPotentSulfurTicker(state)) {
                addTicker(x, y, z, minHeight);
            }
        }
    }

    private boolean hasPotentSulfurTicker(WrappedBlockState state) {
        if (state.getType() != StateTypes.POTENT_SULFUR) {
            return false;
        }

        return state.getPotentSulfurState() != PotentSulfurState.DRY;
    }

    private void addTicker(int x, int y, int z, int minHeight) {
        Long2ObjectMap<GeyserBlockEntity> section = this.tickersBySection.computeIfAbsent(
                sectionKey(x >> 4, z >> 4, (y - minHeight) >> 4),
                ignored -> new Long2ObjectOpenHashMap<>()
        );

        long pos = GrimMath.asLong(x, y, z);
        if (!section.containsKey(pos)) {
            section.put(pos, new GeyserBlockEntity(x, y, z, this.tickOrder++));
        }
    }

    private void removeTicker(int x, int y, int z, int minHeight) {
        removeFromSection(x, y, z, sectionKey(x >> 4, z >> 4, (y - minHeight) >> 4));
    }

    private void removeSection(int chunkX, int chunkZ, int sectionIndex) {
        this.tickersBySection.remove(sectionKey(chunkX, chunkZ, sectionIndex));
    }

    private void removeFromSection(int x, int y, int z, long sectionKey) {
        Long2ObjectMap<GeyserBlockEntity> section = this.tickersBySection.get(sectionKey);
        if (section != null) {
            section.remove(GrimMath.asLong(x, y, z));

            if (section.isEmpty()) {
                this.tickersBySection.remove(sectionKey);
            }
        }
    }

    private static long sectionKey(int chunkX, int chunkZ, int sectionIndex) {
        return ((chunkX & 0x3FFFFFFL) << 38) | ((chunkZ & 0x3FFFFFFL) << 12) | (sectionIndex & 0xFFFL);
    }

    public record GeyserBlockEntity(int x, int y, int z, long tickOrder) {
        private boolean isInside(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            return x >= minX && x <= maxX
                    && y >= minY && y <= maxY
                    && z >= minZ && z <= maxZ;
        }
    }

}
