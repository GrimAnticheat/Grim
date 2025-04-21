package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;

import java.util.UUID;

public class PacketEntityGuardian extends PacketEntity {
    // this is only actually stored as a field in legacy versions (1.8 - 1.10.2)
    // in newer versions Elder Guardians are separate entities, we use this field regardless for simplicity
    public boolean isElder;

    public PacketEntityGuardian(GrimPlayer player, UUID uuid, EntityType type, double x, double y, double z, boolean isElder) {
        super(player, uuid, type, x, y, z);
        this.isElder = isElder;
    }
}
