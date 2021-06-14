package ac.grim.grimac.utils.data.packetentity;

import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.entity.Entity;

public class PacketEntityHorse extends PacketEntity {

    public boolean isRearing = false;
    public boolean hasSaddle = false;

    public PacketEntityHorse(Entity entity, Vector3d position) {
        super(entity, position);
    }
}
