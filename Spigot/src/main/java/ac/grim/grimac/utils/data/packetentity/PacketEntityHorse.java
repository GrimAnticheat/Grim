package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;

public class PacketEntityHorse extends PacketEntityTrackXRot {
    public boolean isRearing = false;
    public boolean hasSaddle = false;
    public boolean isTame = false;
    public double jumpStrength = 0.7;
    public float movementSpeedAttribute = 0.225f;

    public PacketEntityHorse(GrimPlayer player, EntityType type, double x, double y, double z, float xRot) {
        super(player, type, x, y, z, xRot);

        if (EntityTypes.isTypeInstanceOf(type, EntityTypes.CHESTED_HORSE)) {
            jumpStrength = 0.5;
            movementSpeedAttribute = 0.175f;
        }

        if (type == EntityTypes.ZOMBIE_HORSE || type == EntityTypes.SKELETON_HORSE) {
            movementSpeedAttribute = 0.2f;
        }
    }
}
