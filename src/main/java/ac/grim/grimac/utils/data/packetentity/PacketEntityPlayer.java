package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.utils.enums.Pose;
import io.github.retrooper.packetevents.utils.vector.Vector3d;

public class PacketEntityPlayer extends PacketEntity {
    public Pose pose = Pose.STANDING;

    public PacketEntityPlayer(org.bukkit.entity.EntityType type, Vector3d position) {
        super(type, position);
    }
}
