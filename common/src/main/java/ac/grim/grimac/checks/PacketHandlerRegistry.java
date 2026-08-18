package ac.grim.grimac.checks;

import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class PacketHandlerRegistry<T extends @NotNull ProtocolPacketEvent> {
    private final Map<PacketTypeCommon, ArrayList<Consumer<T>>> handlers = new IdentityHashMap<>();
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

    public void trimToSize() {
        for (ArrayList<Consumer<T>> handlers : handlers.values()) {
            handlers.trimToSize();
        }
        catchAll.trimToSize();
    }

    public void handle(T event) {
        List<Consumer<T>> typed = handlers.get(event.getPacketType());
        if (typed != null) {
            for (Consumer<T> handler : typed) {
                handler.accept(event);
            }
            return;
        }

        for (Consumer<T> handler : catchAll) {
            handler.accept(event);
        }
    }
}
