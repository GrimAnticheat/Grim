package ac.grim.grimac.utils.chunks;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.utils.latency.CompensatedWorld;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v1_16.Chunk_v1_9;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.PaletteType;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class ChunkSectionCache {

    private static final ChunkSectionCache INSTANCE = new ChunkSectionCache();

    public static ChunkSectionCache getInstance() {
        return INSTANCE;
    }

    private record Entry(BaseChunk section, AtomicInteger refs) { }

    public record SharedRef(BaseChunk section, long key) { }

    private final ConcurrentHashMap<Long, Entry> entries = new ConcurrentHashMap<>();

    ChunkSectionCache() {
    }

    public static boolean isSharingEnabled() {
        try {
            return GrimAPI.INSTANCE.getConfigManager().getConfig()
                    .getBooleanElse("experimental-chunk-sharing", false);
        } catch (Exception e) {
            // Config not ready (early startup, unit tests): behave as today
            return false;
        }
    }

    public static long hashSection(BaseChunk section) {
        long hash = 0xcbf29ce484222325L;
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    hash ^= (section.getBlockId(x, y, z) & 0xFFFFFFFFL);
                    hash *= 0x100000001b3L;
                }
            }
        }
        return hash;
    }

    public static boolean sectionsEqual(BaseChunk a, BaseChunk b) {
        if (a == b) return true;
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    if (a.getBlockId(x, y, z) != b.getBlockId(x, y, z)) return false;
                }
            }
        }
        return true;
    }

    public static BaseChunk copySection(BaseChunk source) {
        return copySection(source, emptySectionLike(source));
    }

    private static BaseChunk copySection(BaseChunk source, BaseChunk empty) {
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    empty.set(x, y, z, source.getBlockId(x, y, z));
                }
            }
        }
        return empty;
    }

    private static BaseChunk emptySectionLike(BaseChunk source) {
        if (source instanceof Chunk_v1_18) return new Chunk_v1_18();
        if (source instanceof Chunk_v1_9) return new Chunk_v1_9(0, PaletteType.CHUNK.create());
        throw new IllegalArgumentException("Unsupported section type: " + source.getClass());
    }

    // Merges a partial (non ground-up) section update into an existing column.
    // Incoming sections are interned when chunk sharing is enabled, and any
    // shared section they replace is released.
    public static void mergeIncomingSections(CompensatedWorld world, int chunkX, int chunkZ, BaseChunk[] incoming) {
        Column existing = world.getChunk(chunkX, chunkZ);

        if (existing == null) {
            // Corrupting the player's empty chunk is actually quite meaningless
            // You are able to set blocks inside it, and they do apply, it just always returns air despite what its data says
            // So go ahead, corrupt the player's empty chunk and make it no longer all air, it doesn't matter
            //
            // LogUtil.warn("Invalid non-ground up continuous sent for empty chunk " + chunkX + " " + chunkZ + " for " + player.user.getProfile().getName() + "! This corrupts the player's empty chunk!");
            return;
        }

        boolean share = ChunkSectionCache.isSharingEnabled();
        ChunkSectionCache cache = ChunkSectionCache.getInstance();

        BaseChunk[] current = existing.chunks();
        BaseChunk[] interned = new BaseChunk[incoming.length];

        long[] keys = existing.sectionKeys();
        long[] internedKeys = new long[incoming.length];

        for (int i = 0; i < incoming.length; i++) {
            if (incoming[i] == null) continue;
            if (share) {
                ChunkSectionCache.SharedRef ref = cache.internRef(incoming[i]);
                interned[i] = ref.section();
                internedKeys[i] = ref.key();
            } else {
                interned[i] = incoming[i];
            }
        }

        for (int i = 0; i < current.length && i < incoming.length; i++) {
            if (interned[i] == null) continue;
            if (keys[i] != 0L) cache.release(keys[i], current[i]);
            current[i] = interned[i];
            keys[i] = internedKeys[i];
        }
    }

    public static Column shareColumnSections(Column column) {
        if (!ChunkSectionCache.isSharingEnabled()) return column;

        ChunkSectionCache cache = ChunkSectionCache.getInstance();
        BaseChunk[] sections = column.chunks();

        long[] keys = column.sectionKeys();

        for (int i = 0; i < sections.length; i++) {
            if (sections[i] == null) continue;

            // Checks if there is already a shared refenrece
            ChunkSectionCache.SharedRef ref = cache.internRef(sections[i]);
            sections[i] = ref.section();
            keys[i] = ref.key();
        }

        return column;
    }

    public static void releaseColumnSections(Column column) {
        BaseChunk[] sections = column.chunks();
        long[] keys = column.sectionKeys();

        if (sections == null || keys == null) return;

        ChunkSectionCache cache = ChunkSectionCache.getInstance();

        for (int i = 0; i < sections.length && i < keys.length; i++) {
            if (keys[i] != 0L) {
                cache.release(keys[i], sections[i]);
                keys[i] = 0L;
            }
        }
    }

    public static void releaseSharedSections(CompensatedWorld world) {
        for (Column column : world.chunks.values()) {
            if (column != null) releaseColumnSections(column);
        }
    }

    public static BaseChunk detachSectionForWrite(Column column, int sectionIndex, BaseChunk section) {
        long[] sectionKeys = column.sectionKeys();

        if (sectionKeys[sectionIndex] == 0L) return section;

        BaseChunk copy = ChunkSectionCache.copySection(section);
        ChunkSectionCache.getInstance().release(sectionKeys[sectionIndex], section);

        column.chunks()[sectionIndex] = copy;
        sectionKeys[sectionIndex] = 0L;

        return copy;
    }

    public SharedRef internRef(BaseChunk fresh) {
        final SharedRef[] result = new SharedRef[1];
        entries.compute(hashSection(fresh), (hash, existing) -> {
            if (existing != null && sectionsEqual(existing.section, fresh)) {
                existing.refs.incrementAndGet();
                result[0] = new SharedRef(existing.section, hash);
                return existing;
            }

            if (existing == null) {
                result[0] = new SharedRef(fresh, hash);
                return new Entry(fresh, new AtomicInteger(1));
            }

            // Genuine hash collision: keep the stored entry, newcomer stays private
            result[0] = new SharedRef(fresh, 0L);
            return existing;
        });
        return result[0];
    }

    public void release(long key, BaseChunk section) {
        if (key == 0L) return; // private section, never entered the cache
        entries.computeIfPresent(key, (hash, existing) -> {
            if (existing.section != section) return existing; // not ours, leave it
            return existing.refs.decrementAndGet() <= 0 ? null : existing;
        });
    }
}
