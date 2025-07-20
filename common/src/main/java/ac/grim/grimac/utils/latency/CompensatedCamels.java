package ac.grim.grimac.utils.latency;

import ac.grim.grimac.utils.data.packetentity.PacketEntityCamel;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class CompensatedCamels {

    private final Int2ObjectMap<PacketEntityCamel> camels = new Int2ObjectOpenHashMap<>();

    public void tick() {
        if (camels.isEmpty()) return;
        for (PacketEntityCamel camel : camels.values()) {
            camel.dashCooldown = Math.max(0, camel.dashCooldown - 1);
        }
    }

    public void addCamel(int entityId, PacketEntityCamel camel) {
        camels.put(entityId, camel);
    }

    public void removeCamel(int entityId) {
        camels.remove(entityId);
    }

}
