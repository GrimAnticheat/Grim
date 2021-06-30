package ac.grim.grimac.utils.data.packetentity;

import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

public class PacketEntityHorse extends PacketEntity {

    public boolean isRearing = false;
    public boolean hasSaddle = false;
    public float jumpStrength = 0.7f;
    public float movementSpeedAttribute = 0.1f;

    public PacketEntityHorse(EntityType type, Vector3d position) {
        super(type, position);
    }
}
