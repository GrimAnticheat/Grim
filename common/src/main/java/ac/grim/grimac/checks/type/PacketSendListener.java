package ac.grim.grimac.checks.type;

import ac.grim.grimac.checks.PacketHandlerRegistry;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import org.jetbrains.annotations.NotNull;

public interface PacketSendListener {
    void registerSend(@NotNull PacketHandlerRegistry<PacketSendEvent> registry);
}
