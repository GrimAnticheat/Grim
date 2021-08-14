package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.utils.enums.EntityType;
import io.github.retrooper.packetevents.utils.vector.Vector3d;

import java.util.Locale;

public class PacketEntity {
    public EntityType type;
    public org.bukkit.entity.EntityType bukkitEntityType;
    public Vector3d lastTickPosition;
    public Vector3d position;
    public PacketEntity riding;
    public int[] passengers = new int[0];
    public boolean isDead = false;
    public boolean isBaby = false;
    public boolean hasGravity = true;

    public PacketEntity(org.bukkit.entity.EntityType type, Vector3d position) {
        this.position = position;
        this.lastTickPosition = position;
        this.bukkitEntityType = type;
        this.type = EntityType.valueOf(type.toString().toUpperCase(Locale.ROOT));
    }

    public boolean hasPassenger(int entityID) {
        for (int passenger : passengers) {
            if (passenger == entityID) return true;
        }
        return false;
    }
}
