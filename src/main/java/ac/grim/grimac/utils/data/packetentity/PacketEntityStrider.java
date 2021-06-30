package ac.grim.grimac.utils.data.packetentity;

import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.entity.EntityType;

public class PacketEntityStrider extends PacketEntityRideable {
    public boolean isShaking = false;

    public PacketEntityStrider(EntityType type, Vector3d vector3d) {
        super(type, vector3d);
    }
}
