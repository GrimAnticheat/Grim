package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.player.GrimPlayer;

public class PacketEntityRideable extends PacketEntity {

    public boolean hasSaddle = false;
    public int boostTimeMax = 0;
    public int currentBoostTime = 0;

    public float movementSpeedAttribute = 0.1f;

    public PacketEntityRideable(GrimPlayer player, ac.grim.grimac.utils.enums.EntityType type, double x, double y, double z) {
        super(player, type, x, y, z);
    }
}
