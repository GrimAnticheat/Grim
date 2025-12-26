package ac.grim.grimac.utils.latency;

import ac.grim.grimac.utils.data.packetentity.DashableEntity;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class CompensatedDashableEntities {

    private final Int2ObjectMap<DashableEntity> dashableMap = new Int2ObjectOpenHashMap<>();

    public void tick() {
        if (dashableMap.isEmpty()) return;
        for (DashableEntity dashable : dashableMap.values()) {
            dashable.setDashCooldown(Math.max(0, dashable.getDashCooldown() - 1));
        }
    }

    public void addEntity(int entityId, DashableEntity dashable) {
        dashableMap.put(entityId, dashable);
    }

    public void removeEntity(int entityId) {
        dashableMap.remove(entityId);
    }

}
