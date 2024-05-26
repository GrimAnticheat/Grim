package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;

public class PacketEntityHook extends PacketEntity{
    public int owner;
    public int attached = -1;

    public PacketEntityHook(final GrimPlayer player, final EntityType type,
                            final double x, final double y, final double z, final int owner) {
        super(player, type, x, y, z);
        this.owner = owner;
    }
}
