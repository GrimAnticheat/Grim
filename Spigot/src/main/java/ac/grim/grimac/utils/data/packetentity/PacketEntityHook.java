package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;

public class PacketEntityHook extends PacketEntity{
    public int owner;
    public int attached = -1;

    public PacketEntityHook(GrimPlayer player, EntityType type, double x, double y, double z, int owner) {
        super(player, type, x, y, z);
        this.owner = owner;
    }
}
