package ac.grim.grimac.checks.packethandler;

import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import org.jetbrains.annotations.NotNull;

public interface PacketHandler<T extends @NotNull ProtocolPacketEvent> {
    void handle(T event, @NotNull LazyPacketWrapper wrapper);
}
