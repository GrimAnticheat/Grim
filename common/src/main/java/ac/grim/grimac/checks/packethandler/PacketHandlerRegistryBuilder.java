package ac.grim.grimac.checks.packethandler;

import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.function.Consumer;

public class PacketHandlerRegistryBuilder<T extends @NotNull ProtocolPacketEvent> {
    private final IdentityHashMap<PacketTypeCommon, ArrayList<Consumer<T>>> handlers = new IdentityHashMap<>();
    private final ArrayList<Consumer<T>> catchAll = new ArrayList<>();

    public void registerHandler(Consumer<T> consumer, PacketTypeCommon... types) {
        if (types.length == 0) {
            catchAll.add(consumer);
            // A catchAll registered before a typed handler has to run before it.
            for (ArrayList<Consumer<T>> typed : handlers.values()) {
                typed.add(consumer);
            }
            return;
        }

        for (PacketTypeCommon type : types) {
            handlers.computeIfAbsent(type, ignored -> new ArrayList<>(catchAll)).add(consumer);
        }
    }

    public PacketHandlerRegistry<T> build() {
        return new PacketHandlerRegistry<>(handlers, catchAll);
    }
}
