package ac.grim.grimac.utils.data.packetentity;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityProperties;

import java.util.ArrayList;

public class PacketEntitySelf extends PacketEntity {
    public WrapperPlayServerEntityProperties.Property playerSpeed = new WrapperPlayServerEntityProperties.Property("MOVEMENT_SPEED", 0.1f, new ArrayList<>());

    public PacketEntitySelf() {
        super(EntityTypes.PLAYER);
    }

    public boolean inVehicle() {
        return getRiding() != null;
    }
}
