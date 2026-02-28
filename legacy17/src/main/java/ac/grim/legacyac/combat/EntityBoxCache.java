package ac.grim.legacyac.combat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

public final class EntityBoxCache {
    private final Map<EntityType, double[]> cache = new ConcurrentHashMap<EntityType, double[]>();

    public double[] getSize(Entity entity) {
        EntityType type = entity.getType();
        double[] found = cache.get(type);
        if (found != null) {
            return found;
        }

        double width = 0.6D;
        double height = 1.8D;

        switch (type) {
            case PLAYER:
                width = 0.6D;
                height = 1.8D;
                break;
            case ZOMBIE:
            case SKELETON:
            case CREEPER:
                width = 0.6D;
                height = 1.95D;
                break;
            case SPIDER:
                width = 1.4D;
                height = 0.9D;
                break;
            default:
                break;
        }

        double[] size = new double[] { width, height };
        cache.put(type, size);
        return size;
    }
}
