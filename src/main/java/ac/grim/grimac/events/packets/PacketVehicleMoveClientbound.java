package ac.grim.grimac.events.packets;

import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.event.priority.PacketEventPriority;

public class PacketVehicleMoveClientbound extends PacketListenerDynamic {
    public PacketVehicleMoveClientbound() {
        super(PacketEventPriority.MONITOR);
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        //if (event.getPacketId() == PacketType.Play.Server.CHAT) return;
        //Bukkit.broadcastMessage(event.getPacketName());


    }
}
