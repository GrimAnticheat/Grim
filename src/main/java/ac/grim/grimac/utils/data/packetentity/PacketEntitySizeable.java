package ac.grim.grimac.utils.data.packetentity;

import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.entity.EntityType;

public class PacketEntitySizeable extends PacketEntity {
    public int size = 1;

    public PacketEntitySizeable(EntityType type, Vector3d position) {
        super(type, position);
    }
}
