package ac.grim.grimac.utils.data.packetentity;

import org.bukkit.entity.Entity;

public class PacketEntityRideable extends PacketEntity {

    boolean hasSaddle = false;
    int boostTimeMax = 0;
    int currentBoostTime = 0;

    public PacketEntityRideable(Entity entity) {
        super(entity);
    }
}
