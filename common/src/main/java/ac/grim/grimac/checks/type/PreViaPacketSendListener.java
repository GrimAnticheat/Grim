package ac.grim.grimac.checks.type;

import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import org.jetbrains.annotations.NotNull;

public interface PreViaPacketSendListener {
    void registerPreViaSend(@NotNull PacketHandlerRegistryBuilder<PacketSendEvent> registry);
}
