package ac.grim.grimac.checks.packethandler;

import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PacketHandlerRegistry<T extends @NotNull ProtocolPacketEvent> {
    private final IdentityHashMap<PacketTypeCommon, Consumer<T>[]> handlers;
    private final Consumer<T>[] catchAll;

    @SuppressWarnings({"ToArrayCallWithZeroLengthArrayArgument", "unchecked"})
    PacketHandlerRegistry(@NotNull IdentityHashMap<PacketTypeCommon, ArrayList<Consumer<T>>> handlers, @NotNull ArrayList<Consumer<T>> catchAll) {
        this.handlers = new IdentityHashMap<>(handlers.size());
        this.catchAll = catchAll.toArray(new Consumer[catchAll.size()]);

        for (Map.Entry<PacketTypeCommon, ArrayList<Consumer<T>>> entry : handlers.entrySet()) {
            ArrayList<Consumer<T>> value = entry.getValue();
            this.handlers.put(entry.getKey(), value.toArray(new Consumer[value.size()]));
        }
    }

    public void handle(T event) {
        Consumer<T>[] typed = handlers.get(event.getPacketType());
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
