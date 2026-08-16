package ac.grim.grimac.checks.type;

import ac.grim.grimac.checks.PacketHandlerRegistry;
import com.github.retrooper.packetevents.event.PacketSendEvent;

public interface PacketSendListener {
    default void registerSend(PacketHandlerRegistry<PacketSendEvent> registry) {
        registry.registerHandler(this::onPacketSend);
    }

    default void onPacketSend(PacketSendEvent event) {
    }
}
