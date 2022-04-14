package ac.grim.grimac.events.packets;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;

public class PacketSetWrapperNull extends PacketListenerAbstract {
    // It's faster (and less buggy) to simply not re-encode the wrapper unless we changed something
    // The two packets we change are clientbound entity metadata (to fix a netcode issue)
    // and the serverbound player flying packets (to patch NoFall)
    public PacketSetWrapperNull() {
        super(PacketListenerPriority.HIGHEST);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {

    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {

    }
}
