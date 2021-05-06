package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.bukkit.entity.Entity;

public class CompensatedEntities {
    private final Long2ObjectMap<org.bukkit.entity.Entity> entityMap = new Long2ObjectOpenHashMap<>();
    GrimPlayer player;

    public CompensatedEntities(GrimPlayer player) {
        this.player = player;
    }

    public void addEntity(Entity entity) {
        entityMap.put(entity.getEntityId(), entity);
    }

    public void removeEntity(int[] removedEntities) {
        for (int i : removedEntities) {
            entityMap.remove(i);
        }
    }
}
