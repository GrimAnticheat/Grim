package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;

import java.util.UUID;

public class PacketEntityCamel extends PacketEntityHorse {

    public boolean dashing = false;

    public PacketEntityCamel(GrimPlayer player, UUID uuid, EntityType type, double x, double y, double z, float xRot) {
        super(player, uuid, type, x, y, z, xRot);

        setAttribute(Attributes.JUMP_STRENGTH, 0.42f);
        setAttribute(Attributes.MOVEMENT_SPEED, 0.09f);
        setAttribute(Attributes.STEP_HEIGHT, 1.5f);
    }
}
