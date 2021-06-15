package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.utils.enums.EntityType;
import ac.grim.grimac.utils.enums.Pose;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.entity.Entity;

import java.util.Locale;

public class PacketEntity {
    public Entity entity;
    public EntityType type;
    public Pose pose = Pose.STANDING;
    public Vector3d lastTickPosition;
    public Vector3d position;
    public PacketEntity riding;
    public int[] passengers;
    public boolean isDead = false;

    public PacketEntity(Entity entity, Vector3d position) {
        this.entity = entity;
        this.position = position;
        this.lastTickPosition = position;
        this.type = EntityType.valueOf(entity.getType().toString().toUpperCase(Locale.ROOT));
    }
}
