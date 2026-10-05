package ac.grim.grimac.utils.chunks;

import ac.grim.grimac.GrimAPI;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v1_16.Chunk_v1_9;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.DataPalette;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class ChunkSectionCache {

    private static final ChunkSectionCache INSTANCE = new ChunkSectionCache();

    public static ChunkSectionCache getInstance() {
        return INSTANCE;
    }

    private static final class Entry {
        final BaseChunk section;
        final AtomicInteger refs = new AtomicInteger(1);

        Entry(BaseChunk section) {
            this.section = section;
        }
    }

    public record SharedRef(BaseChunk section, long key) { }

    public record Stats(long entries, long totalRefs, long hits, long misses, long collisions) { }

    private final ConcurrentHashMap<Long, Entry> entries = new ConcurrentHashMap<>();
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    private final AtomicLong collisions = new AtomicLong();

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

    static BaseChunk copySection(BaseChunk source, BaseChunk empty) {
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
        if (source instanceof Chunk_v1_9) return new Chunk_v1_9(0, DataPalette.createForChunk());
        throw new IllegalArgumentException("Unsupported section type: " + source.getClass());
    }

    /**
     * Returns the shared instance for this content, registering a new entry when unseen.
     */
    public SharedRef internRef(BaseChunk fresh) {
        final SharedRef[] result = new SharedRef[1];
        entries.compute(hashSection(fresh), (hash, existing) -> {
            if (existing != null && sectionsEqual(existing.section, fresh)) {
                existing.refs.incrementAndGet();
                hits.incrementAndGet();
                result[0] = new SharedRef(existing.section, hash);
                return existing;
            }

            if (existing == null) {
                misses.incrementAndGet();
                result[0] = new SharedRef(fresh, hash);
                return new Entry(fresh);
            }

            // Genuine hash collision: keep the stored entry, newcomer stays private
            collisions.incrementAndGet();
            result[0] = new SharedRef(fresh, 0L);
            return existing;
        });
        return result[0];
    }

    /**
     * Releases one reference previously obtained via {@link #internRef}.
     */
    public void release(long key, BaseChunk section) {
        if (key == 0L) return; // private section, never entered the cache
        entries.computeIfPresent(key, (hash, existing) -> {
            if (existing.section != section) return existing; // not ours, leave it
            return existing.refs.decrementAndGet() <= 0 ? null : existing;
        });
    }

    public Stats stats() {
        long totalRefs = 0;

        for (Entry entry : entries.values()) {
            totalRefs += entry.refs.get();
        }

        return new Stats(entries.size(), totalRefs, hits.get(), misses.get(), collisions.get());
    }
}
