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
    private final Map<PacketTypeCommon, List<Consumer<T>>> handlers = new IdentityHashMap<>();
    private final List<Consumer<T>> catchAll = new ArrayList<>();

    public void registerHandler(Consumer<T> consumer, PacketTypeCommon... types) {
        if (types.length == 0) {
            catchAll.add(consumer);
            return;
        }

        for (PacketTypeCommon type : types) {
            handlers.computeIfAbsent(type, ignored -> new ArrayList<>()).add(consumer);
        }
    }

    public void handle(T event) {
        List<Consumer<T>> typed = handlers.get(event.getPacketType());
        if (typed != null) {
            for (Consumer<T> handler : typed) {
                handler.accept(event);
            }
        }

        for (Consumer<T> handler : catchAll) {
            handler.accept(event);
        }
    }
}
