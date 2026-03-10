package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.attribute.ValuedAttribute;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;

import java.util.UUID;

public class PacketEntityHappyGhast extends PacketEntityTrackXRot {

    public PacketEntityHappyGhast(GrimPlayer player, UUID uuid, EntityType type, double x, double y, double z, float xRot) {
        super(player, uuid, type, x, y, z, xRot);
        this.trackEntityEquipment = true;

        trackAttribute(ValuedAttribute.ranged(Attributes.FLYING_SPEED, 0.05, 0, 1024));
        trackAttribute(ValuedAttribute.ranged(Attributes.MOVEMENT_SPEED, 0.05, 0, 1024));
    }

    public boolean isControllingPassenger() {
        return this.isWearingBodyArmor() && this.getFirstPassenger() instanceof PacketEntitySelf;
    }

    public boolean isWearingBodyArmor() {
        return this.hasItemInSlot(EquipmentSlot.BODY);
    }

    public PacketEntity getFirstPassenger() {
        return this.passengers.isEmpty() ? null : this.passengers.get(0);
    }

}
