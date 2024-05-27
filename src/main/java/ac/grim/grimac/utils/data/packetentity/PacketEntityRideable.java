package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;

public class PacketEntityRideable extends PacketEntity {

    public boolean hasSaddle = false;
    public int boostTimeMax = 0;
    public int currentBoostTime = 0;

    public float movementSpeedAttribute = 0.1f;

    public PacketEntityRideable(final GrimPlayer player, final EntityType type,
                                final double x, final double y, final double z) {
        super(player, type, x, y, z);
        this.stepHeight = 1.0f;
    }
}
