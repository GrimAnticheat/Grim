package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.utils.enums.Pose;
import org.bukkit.entity.Entity;

public class PacketEntity {
    public Entity entity;
    Pose pose = Pose.STANDING;

    public PacketEntity(Entity entity) {
        this.entity = entity;
    }
}
