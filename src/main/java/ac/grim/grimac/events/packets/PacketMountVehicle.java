package ac.grim.grimac.events.packets;

import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.mount.WrappedPacketOutMount;

public class PacketMountVehicle extends PacketListenerAbstract {

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Server.MOUNT) {
            WrappedPacketOutMount mount = new WrappedPacketOutMount(event.getNMSPacket());

            // TODO: Handle setting player vehicles, requires entity replication which isn't done yet
        }
    }
}
