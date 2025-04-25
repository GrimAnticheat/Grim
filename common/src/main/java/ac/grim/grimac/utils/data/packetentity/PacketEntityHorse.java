package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.attribute.ValuedAttribute;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
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
        setAttribute(Attributes.STEP_HEIGHT, 1.0f);

        final boolean preAttribute = player.getClientVersion().isOlderThan(ClientVersion.V_1_20_5);
        // This was horse.jump_strength pre-attribute
        trackAttribute(ValuedAttribute.ranged(Attributes.JUMP_STRENGTH, 0.7, 0, preAttribute ? 2 : 32)
                .withSetRewriter((oldValue, newValue) -> {
                    // Seems viabackwards doesn't rewrite this (?)
                    if (preAttribute && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_20_5)) {
                        return oldValue;
                    }
                    // Modern player OR an old server setting legacy horse.jump_strength attribute
                    return newValue;
                }));
        trackAttribute(ValuedAttribute.ranged(Attributes.MOVEMENT_SPEED, 0.225f, 0, 1024));

        if (EntityTypes.isTypeInstanceOf(type, EntityTypes.CHESTED_HORSE)) {
            setAttribute(Attributes.JUMP_STRENGTH, 0.5);
            setAttribute(Attributes.MOVEMENT_SPEED, 0.175f);
        }

        if (type == EntityTypes.ZOMBIE_HORSE || type == EntityTypes.SKELETON_HORSE) {
            setAttribute(Attributes.MOVEMENT_SPEED, 0.2f);
        }
    }
}
