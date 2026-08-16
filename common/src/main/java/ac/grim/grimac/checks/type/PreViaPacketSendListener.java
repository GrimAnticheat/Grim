package ac.grim.grimac.checks.type;

import ac.grim.grimac.checks.PacketHandlerRegistry;
import com.github.retrooper.packetevents.event.PacketSendEvent;

public interface PreViaPacketSendListener {
    default void registerPreViaSend(PacketHandlerRegistry<PacketSendEvent> registry) {
        registry.registerHandler(this::onPreViaPacketSend);
    }

    default void onPreViaPacketSend(PacketSendEvent event) {
    }
}
