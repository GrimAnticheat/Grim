package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;

public class PacketEntityFishingHook extends PacketEntity {
    public int owner;

    public PacketEntityFishingHook(GrimPlayer player, EntityType type, double x, double y, double z) {
        super(player, type, x, y, z);
    }
}
