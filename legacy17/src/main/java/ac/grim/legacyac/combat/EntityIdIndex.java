package ac.grim.legacyac.combat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import org.bukkit.entity.Entity;

public final class EntityIdIndex {
    private static final long LOG_INTERVAL = 200L;

    private final Map<Integer, Entity> entitiesById = new ConcurrentHashMap<Integer, Entity>();
    private final AtomicLong lookups = new AtomicLong();
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    private final AtomicLong fallbackScans = new AtomicLong();
    private final Logger logger;

    public EntityIdIndex(Logger logger) {
        this.logger = logger;
    }

    public Entity get(int entityId) {
        long totalLookups = lookups.incrementAndGet();
        Entity entity = entitiesById.get(Integer.valueOf(entityId));
        if (entity != null && entity.isValid()) {
            hits.incrementAndGet();
            maybeLog(totalLookups);
            return entity;
        }
        entitiesById.remove(Integer.valueOf(entityId));
        misses.incrementAndGet();
        maybeLog(totalLookups);
        return null;
    }

    public void put(Entity entity) {
        if (entity == null) {
            return;
        }
        entitiesById.put(Integer.valueOf(entity.getEntityId()), entity);
    }

    public void remove(Entity entity) {
        if (entity == null) {
            return;
        }
        entitiesById.remove(Integer.valueOf(entity.getEntityId()));
    }

    public void recordFallbackScan() {
        fallbackScans.incrementAndGet();
    }

    private void maybeLog(long totalLookups) {
        if (totalLookups % LOG_INTERVAL != 0L) {
            return;
        }
        long totalHits = hits.get();
        long totalMisses = misses.get();
        long totalFallbackScans = fallbackScans.get();
        double hitRate = totalLookups == 0L ? 100.0D : (totalHits * 100.0D / totalLookups);
        logger.info(String.format("[GLAC] EntityIdIndex hit-rate: %.2f%% (%d/%d), misses=%d, fallback-scans=%d", Double.valueOf(hitRate), Long.valueOf(totalHits), Long.valueOf(totalLookups), Long.valueOf(totalMisses), Long.valueOf(totalFallbackScans)));
    }
}
