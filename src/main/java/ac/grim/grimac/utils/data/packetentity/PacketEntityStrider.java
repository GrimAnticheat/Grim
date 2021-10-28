package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.player.GrimPlayer;

public class PacketEntityStrider extends PacketEntityRideable {
    public boolean isShaking = false;

    public PacketEntityStrider(GrimPlayer player, ac.grim.grimac.utils.enums.EntityType type, double x, double y, double z) {
        super(player, type, x, y, z);
    }
}
