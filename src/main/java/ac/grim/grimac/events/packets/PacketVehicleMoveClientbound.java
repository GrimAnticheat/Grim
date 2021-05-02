package ac.grim.grimac.events.packets;

import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.event.priority.PacketEventPriority;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.position.WrappedPacketOutPosition;
import org.bukkit.Bukkit;

public class PacketVehicleMoveClientbound extends PacketListenerDynamic {
    public PacketVehicleMoveClientbound() {
        super(PacketEventPriority.MONITOR);
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        if (event.getPacketId() == PacketType.Play.Server.CHAT) return;
        Bukkit.broadcastMessage(event.getPacketName());

        if (event.getPacketId() == PacketType.Play.Server.POSITION) {
            WrappedPacketOutPosition teleport = new WrappedPacketOutPosition(event.getNMSPacket());

            Bukkit.broadcastMessage("Teleporting to " + teleport.getPosition().toString());
            //Bukkit.broadcastMessage("TELEPORT " + teleport.getPosition().toString());
        }
    }
}
