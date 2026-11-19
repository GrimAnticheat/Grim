package ac.grim.grimac.checks.packethandler;

import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder.IncompleteHandlerMap;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class PacketHandlerRegistry {
    private final PacketMap<@NotNull Consumer<@NotNull PacketSendEvent>[]> sendHandlers;
    private final PacketMap<@NotNull Consumer<@NotNull PacketSendEvent>[]> preViaSendHandlers;
    private final PacketMap<@NotNull Consumer<@NotNull PacketReceiveEvent>[]> receiveHandlers;
    private final PacketMap<@NotNull Consumer<@NotNull PacketReceiveEvent>[]> preViaReceiveHandlers;
    private final PacketMap<@NotNull Consumer<@NotNull PacketReceiveEvent>[]> prePredictionReceiveHandlers;

    PacketHandlerRegistry(
            @NotNull IncompleteHandlerMap<PacketSendEvent> sendHandlers,
            @NotNull IncompleteHandlerMap<PacketSendEvent> preViaSendHandlers,
            @NotNull IncompleteHandlerMap<PacketReceiveEvent> receiveHandlers,
            @NotNull IncompleteHandlerMap<PacketReceiveEvent> preViaReceiveHandlers,
            @NotNull IncompleteHandlerMap<PacketReceiveEvent> prePredictionReceiveHandlers
    ) {
        this.sendHandlers = sendHandlers.complete();
        this.preViaSendHandlers = preViaSendHandlers.complete();
        this.receiveHandlers = receiveHandlers.complete();
        this.preViaReceiveHandlers = preViaReceiveHandlers.complete();
        this.prePredictionReceiveHandlers = prePredictionReceiveHandlers.complete();
    }

    public void handleSend(@NotNull PacketSendEvent event) {
        handle(event, sendHandlers);
    }

    public void handlePreViaSend(@NotNull PacketSendEvent event) {
        handle(event, preViaSendHandlers);
    }

    public void handleReceive(@NotNull PacketReceiveEvent event) {
        handle(event, receiveHandlers);
    }

    public void handlePreViaReceive(@NotNull PacketReceiveEvent event) {
        handle(event, preViaReceiveHandlers);
    }

    public void handlePrePredictionReceive(@NotNull PacketReceiveEvent event) {
        handle(event, prePredictionReceiveHandlers);
    }

    private <E extends @NotNull ProtocolPacketEvent> void handle(
            E event,
            @NotNull PacketMap<@NotNull Consumer<E> @NotNull []> handlers
    ) {
        Consumer<E>[] array = handlers.get(event.getPacketType(), event.getConnectionState());
        if (array == null) return; // we don't care about this packet
        for (Consumer<E> handler : array) {
            handler.accept(event);
        }
    }
}
