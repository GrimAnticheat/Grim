package ac.grim.grimac.utils.data.packetentity;

import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

public class PacketEntityRideable extends PacketEntity {

    public boolean hasSaddle = false;
    public int boostTimeMax = 0;
    public int currentBoostTime = 1;

    public float movementSpeedAttribute = 0.1f;

    public PacketEntityRideable(EntityType type, Vector3d vector3d) {
        super(type, vector3d);
    }
}
