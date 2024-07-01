package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.attribute.ValuedAttribute;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

import java.util.UUID;

public class PacketEntityHorse extends PacketEntityTrackXRot {

    public boolean isRearing = false;
    public boolean hasSaddle = false;
    public boolean isTame = false;

    public PacketEntityHorse(GrimPlayer player, UUID uuid, EntityType type, double x, double y, double z, float xRot) {
        super(player, uuid, type, x, y, z, xRot);
        getAttribute(Attributes.GENERIC_STEP_HEIGHT).override(1.0f);

        final boolean preAttribute = player.getClientVersion().isOlderThan(ClientVersion.V_1_20_5);
        attributeMap.put(Attributes.GENERIC_JUMP_STRENGTH, ValuedAttribute.ranged(Attributes.GENERIC_JUMP_STRENGTH, 0.7, 0, preAttribute ? 2 : 32));
        attributeMap.put(Attributes.GENERIC_MOVEMENT_SPEED, ValuedAttribute.ranged(Attributes.GENERIC_MOVEMENT_SPEED, 0.225f, 0, 1024));

        if (EntityTypes.isTypeInstanceOf(type, EntityTypes.CHESTED_HORSE)) {
            getAttribute(Attributes.GENERIC_JUMP_STRENGTH).override(0.5);
            getAttribute(Attributes.GENERIC_MOVEMENT_SPEED).override(0.175f);
        }

        if (type == EntityTypes.ZOMBIE_HORSE || type == EntityTypes.SKELETON_HORSE) {
            getAttribute(Attributes.GENERIC_MOVEMENT_SPEED).override(0.2f);
        }
    }
}
