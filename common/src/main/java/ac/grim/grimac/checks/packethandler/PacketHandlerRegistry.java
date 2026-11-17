package ac.grim.grimac.checks.packethandler;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.function.Consumer;

public class PacketHandlerRegistry {
    private final PacketMap<Consumer<PacketSendEvent>[]> sendHandlers;
    private final PacketMap<Consumer<PacketSendEvent>[]> preViaSendHandlers;
    private final PacketMap<Consumer<PacketReceiveEvent>[]> receiveHandlers;
    private final PacketMap<Consumer<PacketReceiveEvent>[]> preViaReceiveHandlers;

    @SuppressWarnings("ToArrayCallWithZeroLengthArrayArgument")
    PacketHandlerRegistry(
            @NotNull PacketMap<ArrayList<Consumer<PacketSendEvent>>> sendHandlers,
            @NotNull PacketMap<ArrayList<Consumer<PacketSendEvent>>> preViaSendHandlers,
            @NotNull PacketMap<ArrayList<Consumer<PacketReceiveEvent>>> receiveHandlers,
            @NotNull PacketMap<ArrayList<Consumer<PacketReceiveEvent>>> preViaReceiveHandlers,
            @NotNull ArrayList<Consumer<PacketSendEvent>> sendDefaults,
            @NotNull ArrayList<Consumer<PacketSendEvent>> preViaSendDefaults,
            @NotNull ArrayList<Consumer<PacketReceiveEvent>> receiveDefaults,
            @NotNull ArrayList<Consumer<PacketReceiveEvent>> preViaReceiveDefaults
    ) {
        Object[] sendDefault = sendDefaults.toArray(new Consumer[sendDefaults.size()]);
        this.sendHandlers = sendHandlers.mapRawValues(value -> value == null ? sendDefault : value.toArray(new Consumer[value.size()]));
        Object[] preViaSendDefault = preViaSendDefaults.toArray(new Consumer[preViaSendDefaults.size()]);
        this.preViaSendHandlers = preViaSendHandlers.mapRawValues(value -> value == null ? preViaSendDefault : value.toArray(new Consumer[value.size()]));
        Object[] receiveDefault = receiveDefaults.toArray(new Consumer[receiveDefaults.size()]);
        this.receiveHandlers = receiveHandlers.mapRawValues(value -> value == null ? receiveDefault : value.toArray(new Consumer[value.size()]));
        Object[] preViaReceiveDefault = preViaReceiveDefaults.toArray(new Consumer[preViaReceiveDefaults.size()]);
        this.preViaReceiveHandlers = preViaReceiveHandlers.mapRawValues(value -> value == null ? preViaReceiveDefault : value.toArray(new Consumer[value.size()]));
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
