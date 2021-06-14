package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.utils.enums.Pose;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.entity.Entity;

public class PacketEntity {
    public Entity entity;
    public Pose pose = Pose.STANDING;
    public Vector3d position;

    public PacketEntity(Entity entity, Vector3d position) {
        this.entity = entity;
        this.position = position;
    }
}
