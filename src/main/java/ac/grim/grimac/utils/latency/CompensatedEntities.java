package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import org.bukkit.entity.Entity;

public class CompensatedEntities {
    private final Int2ObjectLinkedOpenHashMap<Entity> entityMap = new Int2ObjectLinkedOpenHashMap<>();
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
