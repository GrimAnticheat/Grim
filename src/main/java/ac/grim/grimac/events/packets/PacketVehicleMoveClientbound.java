package ac.grim.grimac.events.packets;

import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.event.priority.PacketEventPriority;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.entityteleport.WrappedPacketOutEntityTeleport;

public class PacketVehicleMoveClientbound extends PacketListenerDynamic {
    public PacketVehicleMoveClientbound() {
        super(PacketEventPriority.MONITOR);
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        //if (event.getPacketId() == PacketType.Play.Server.CHAT) return;
        //Bukkit.broadcastMessage(event.getPacketName());

        if (event.getPacketId() == PacketType.Play.Server.ENTITY_TELEPORT) {
            WrappedPacketOutEntityTeleport teleport = new WrappedPacketOutEntityTeleport(event.getNMSPacket());

            //Bukkit.broadcastMessage("TELEPORT " + teleport.getPosition().toString());
        }
    }
}
