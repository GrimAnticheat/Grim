package ac.grim.grimac.checks.type;

import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import org.jetbrains.annotations.NotNull;

public interface PacketListener {
    void registerPacketHandlers(@NotNull PacketHandlerRegistryBuilder registry);
}
