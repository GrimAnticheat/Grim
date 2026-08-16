package ac.grim.grimac.checks.type;

import ac.grim.grimac.checks.PacketHandlerRegistry;
import com.github.retrooper.packetevents.event.PacketSendEvent;

public interface PreViaPacketSendListener {
    void registerPreViaSend(PacketHandlerRegistry<PacketSendEvent> registry);
}
