package ac.grim.grimac.checks.type;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;

@Deprecated
public interface PacketReceiveListener {
    void onPacketReceive(PacketReceiveEvent event);
}
