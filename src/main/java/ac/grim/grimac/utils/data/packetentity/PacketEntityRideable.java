package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.utils.lists.EvictingList;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.entity.EntityType;

public class PacketEntityRideable extends PacketEntity {

    public boolean hasSaddle = false;
    public int boostTimeMax = 0;
    public int currentBoostTime = 0;

    public float movementSpeedAttribute = 0.1f;

    public EvictingList<Vector3d> entityPositions = new EvictingList<>(3);

    public PacketEntityRideable(EntityType type, Vector3d vector3d) {
        super(type, vector3d);
        entityPositions.add(vector3d);
    }
}
