package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;

public class PacketEntityCamel extends PacketEntityHorse {

    public boolean dashing = false; //TODO: handle camel dashing

    public PacketEntityCamel(GrimPlayer player, EntityType type, double x, double y, double z, float xRot) {
        super(player, type, x, y, z, xRot);

        jumpStrength = 0.42F;
        movementSpeedAttribute = 0.09f;
    }

}
