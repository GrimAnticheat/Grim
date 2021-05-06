package ac.grim.grimac.events.packets;

import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.entity.WrappedPacketOutEntity;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitydestroy.WrappedPacketOutEntityDestroy;

public class PacketEntityReplication extends PacketListenerDynamic {

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Server.SPAWN_ENTITY) {
            WrappedPacketOutEntity entity = new WrappedPacketOutEntity(event.getNMSPacket());

        }

        if (packetID == PacketType.Play.Server.ENTITY_METADATA) {
            // PacketPlayOutEntityMetadata
            Object metadata = event.getNMSPacket().getRawNMSPacket();


        }

        if (packetID == PacketType.Play.Server.ENTITY_DESTROY) {
            // PacketPlayOutEntityDestroy
            WrappedPacketOutEntityDestroy destroy = new WrappedPacketOutEntityDestroy(event.getNMSPacket());
        }
    }
}
