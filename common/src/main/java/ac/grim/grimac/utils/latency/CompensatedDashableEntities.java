package ac.grim.grimac.utils.latency;

import ac.grim.grimac.utils.data.packetentity.DashableEntity;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class CompensatedDashableEntities {

    private final Int2ObjectMap<DashableEntity> dashableMap = new Int2ObjectOpenHashMap<>();

    public void tick() {
        if (dashableMap.isEmpty()) return;
        for (DashableEntity camel : dashableMap.values()) {
            camel.setDashCooldown(Math.max(0, camel.getDashCooldown() - 1));
        }
    }

    public void addEntity(int entityId, DashableEntity dashableEntity) {
        dashableMap.put(entityId, dashableEntity);
    }

    public void removeEntity(int entityId) {
        dashableMap.remove(entityId);
    }

}
