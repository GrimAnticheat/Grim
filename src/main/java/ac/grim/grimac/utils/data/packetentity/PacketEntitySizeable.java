package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.player.GrimPlayer;

public class PacketEntitySizeable extends PacketEntity {
    public int size = 1;

    public PacketEntitySizeable(GrimPlayer player, ac.grim.grimac.utils.enums.EntityType type, double x, double y, double z) {
        super(player, type, x, y, z);
    }
}
