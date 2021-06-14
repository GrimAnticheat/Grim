package ac.grim.grimac.utils.data.packetentity;

import org.bukkit.entity.Entity;

public class PacketEntityHorse extends PacketEntity {

    boolean isRearing = false;
    boolean hasSaddle = false;

    public PacketEntityHorse(Entity entity) {
        super(entity);
    }
}
