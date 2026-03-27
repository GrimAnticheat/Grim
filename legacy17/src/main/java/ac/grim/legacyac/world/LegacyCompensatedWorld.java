package ac.grim.legacyac.world;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class LegacyCompensatedWorld {
    private static final long PENDING_TIMEOUT_MS = 2000L;

    private final Map<String, Map<Long, ChunkCache>> worldChunks = new ConcurrentHashMap<String, Map<Long, ChunkCache>>();
    private final LinkedList<PendingChunkRefresh> pendingChunkRefreshes = new LinkedList<PendingChunkRefresh>();
    private final LinkedList<PendingBlockUpdate> pendingBlockUpdates = new LinkedList<PendingBlockUpdate>();

    public void preloadAround(Player player, int radiusChunks) {
        if (player == null) {
            return;
        }
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        int centerChunkX = location.getBlockX() >> 4;
        int centerChunkZ = location.getBlockZ() >> 4;
        for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
            for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
                ensureChunkSnapshot(world, centerChunkX + dx, centerChunkZ + dz);
            }
        }
    }

    public void queueChunkRefresh(World world, int chunkX, int chunkZ, short anchorTransactionId) {
        if (world == null) {
            return;
        }
        synchronized (pendingChunkRefreshes) {
            pendingChunkRefreshes.add(new PendingChunkRefresh(world.getName(), chunkX, chunkZ,
                    anchorTransactionId, System.currentTimeMillis() + PENDING_TIMEOUT_MS));
            trimChunkRefreshes();
        }
    }

    public void queueBlockChange(World world, int x, int y, int z, Material type, byte data, short anchorTransactionId) {
        if (world == null) {
            return;
        }
        synchronized (pendingBlockUpdates) {
            pendingBlockUpdates.add(new PendingBlockUpdate(world.getName(), x, y, z,
                    type == null ? Material.AIR : type, data, anchorTransactionId,
                    System.currentTimeMillis() + PENDING_TIMEOUT_MS));
            trimBlockUpdates();
        }
    }

    public void acknowledgeTransaction(short actionId) {
        if (actionId == 0) {
            return;
        }
        applyAnchoredChunkRefreshes(actionId);
        applyAnchoredBlockUpdates(actionId);
    }

    public LegacyBlockState getBlockState(World world, int x, int y, int z) {
        expirePending();
        if (world == null || y < 0 || y > 255) {
            return LegacyBlockState.AIR;
        }
        ChunkCache chunk = ensureChunkSnapshot(world, x >> 4, z >> 4);
        return chunk.getBlockState(x & 15, y, z & 15);
    }

    public Material getBlockType(World world, int x, int y, int z) {
        return getBlockState(world, x, y, z).getType();
    }

    public byte getBlockData(World world, int x, int y, int z) {
        return getBlockState(world, x, y, z).getData();
    }

    private void applyAnchoredChunkRefreshes(short actionId) {
        synchronized (pendingChunkRefreshes) {
            Iterator<PendingChunkRefresh> iterator = pendingChunkRefreshes.iterator();
            while (iterator.hasNext()) {
                PendingChunkRefresh refresh = iterator.next();
                if (refresh.anchorTransactionId != actionId) {
                    continue;
                }
                World world = Bukkit.getWorld(refresh.worldName);
                if (world != null) {
                    ensureChunkSnapshot(world, refresh.chunkX, refresh.chunkZ).snapshot(world, refresh.chunkX, refresh.chunkZ);
                }
                iterator.remove();
            }
        }
    }

    private void applyAnchoredBlockUpdates(short actionId) {
        synchronized (pendingBlockUpdates) {
            Iterator<PendingBlockUpdate> iterator = pendingBlockUpdates.iterator();
            while (iterator.hasNext()) {
                PendingBlockUpdate update = iterator.next();
                if (update.anchorTransactionId != actionId) {
                    continue;
                }
                World world = Bukkit.getWorld(update.worldName);
                if (world != null) {
                    ChunkCache chunk = ensureChunkSnapshot(world, update.x >> 4, update.z >> 4);
                    chunk.setBlockState(update.x & 15, update.y, update.z & 15, update.type, update.data);
                }
                iterator.remove();
            }
        }
    }

    private void expirePending() {
        long now = System.currentTimeMillis();
        synchronized (pendingChunkRefreshes) {
            Iterator<PendingChunkRefresh> iterator = pendingChunkRefreshes.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().expiresAtMillis <= now) {
                    iterator.remove();
                }
            }
        }
        synchronized (pendingBlockUpdates) {
            Iterator<PendingBlockUpdate> iterator = pendingBlockUpdates.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().expiresAtMillis <= now) {
                    iterator.remove();
                }
            }
        }
    }

    private ChunkCache ensureChunkSnapshot(World world, int chunkX, int chunkZ) {
        Map<Long, ChunkCache> chunks = worldChunks.get(world.getName());
        if (chunks == null) {
            chunks = new ConcurrentHashMap<Long, ChunkCache>();
            Map<Long, ChunkCache> existing = worldChunks.putIfAbsent(world.getName(), chunks);
            if (existing != null) {
                chunks = existing;
            }
        }
        long key = toChunkKey(chunkX, chunkZ);
        ChunkCache cache = chunks.get(key);
        if (cache == null) {
            cache = new ChunkCache();
            ChunkCache existing = chunks.putIfAbsent(key, cache);
            if (existing != null) {
                cache = existing;
            } else {
                cache.snapshot(world, chunkX, chunkZ);
            }
        }
        return cache;
    }

    private static long toChunkKey(int x, int z) {
        return ((x & 0xFFFFFFFFL) << 32L) | (z & 0xFFFFFFFFL);
    }

    private void trimChunkRefreshes() {
        while (pendingChunkRefreshes.size() > 512) {
            pendingChunkRefreshes.removeFirst();
        }
    }

    private void trimBlockUpdates() {
        while (pendingBlockUpdates.size() > 4096) {
            pendingBlockUpdates.removeFirst();
        }
    }

    private static final class PendingChunkRefresh {
        private final String worldName;
        private final int chunkX;
        private final int chunkZ;
        private final short anchorTransactionId;
        private final long expiresAtMillis;

        private PendingChunkRefresh(String worldName, int chunkX, int chunkZ, short anchorTransactionId,
                long expiresAtMillis) {
            this.worldName = worldName;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.anchorTransactionId = anchorTransactionId;
            this.expiresAtMillis = expiresAtMillis;
        }
    }

    private static final class PendingBlockUpdate {
        private final String worldName;
        private final int x;
        private final int y;
        private final int z;
        private final Material type;
        private final byte data;
        private final short anchorTransactionId;
        private final long expiresAtMillis;

        private PendingBlockUpdate(String worldName, int x, int y, int z, Material type, byte data,
                short anchorTransactionId, long expiresAtMillis) {
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.type = type;
            this.data = data;
            this.anchorTransactionId = anchorTransactionId;
            this.expiresAtMillis = expiresAtMillis;
        }
    }
}
