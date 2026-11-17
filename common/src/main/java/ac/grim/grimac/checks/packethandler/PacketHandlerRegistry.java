package ac.grim.grimac.checks.packethandler;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.function.Consumer;

public class PacketHandlerRegistry {
    private final PacketMap<@NotNull Consumer<@NotNull PacketSendEvent>[]> sendHandlers;
    private final PacketMap<@NotNull Consumer<@NotNull PacketSendEvent>[]> preViaSendHandlers;
    private final PacketMap<@NotNull Consumer<@NotNull PacketReceiveEvent>[]> receiveHandlers;
    private final PacketMap<@NotNull Consumer<@NotNull PacketReceiveEvent>[]> preViaReceiveHandlers;
    private final PacketMap<@NotNull Consumer<@NotNull PacketReceiveEvent>[]> prePredictionReceiveHandlers;

    @SuppressWarnings("ToArrayCallWithZeroLengthArrayArgument")
    PacketHandlerRegistry(
            @NotNull PacketMap<@NotNull ArrayList<@NotNull Consumer<@NotNull PacketSendEvent>>> sendHandlers,
            @NotNull PacketMap<@NotNull ArrayList<@NotNull Consumer<@NotNull PacketSendEvent>>> preViaSendHandlers,
            @NotNull PacketMap<@NotNull ArrayList<@NotNull Consumer<@NotNull PacketReceiveEvent>>> receiveHandlers,
            @NotNull PacketMap<@NotNull ArrayList<@NotNull Consumer<@NotNull PacketReceiveEvent>>> preViaReceiveHandlers,
            @NotNull PacketMap<@NotNull ArrayList<@NotNull Consumer<@NotNull PacketReceiveEvent>>> prePredictionReceiveHandlers,
            @NotNull ArrayList<@NotNull Consumer<@NotNull PacketSendEvent>> sendDefaults,
            @NotNull ArrayList<@NotNull Consumer<@NotNull PacketSendEvent>> preViaSendDefaults,
            @NotNull ArrayList<@NotNull Consumer<@NotNull PacketReceiveEvent>> receiveDefaults,
            @NotNull ArrayList<@NotNull Consumer<@NotNull PacketReceiveEvent>> preViaReceiveDefaults,
            @NotNull ArrayList<@NotNull Consumer<@NotNull PacketReceiveEvent>> prePredictionReceiveDefaults
    ) {
        Object[] sendDefault = sendDefaults.toArray(new Consumer[sendDefaults.size()]);
        this.sendHandlers = sendHandlers.mapValuesToRawValues(value -> value == null ? sendDefault : value.toArray(new Consumer[value.size()]));
        Object[] preViaSendDefault = preViaSendDefaults.toArray(new Consumer[preViaSendDefaults.size()]);
        this.preViaSendHandlers = preViaSendHandlers.mapValuesToRawValues(value -> value == null ? preViaSendDefault : value.toArray(new Consumer[value.size()]));
        Object[] receiveDefault = receiveDefaults.toArray(new Consumer[receiveDefaults.size()]);
        this.receiveHandlers = receiveHandlers.mapValuesToRawValues(value -> value == null ? receiveDefault : value.toArray(new Consumer[value.size()]));
        Object[] preViaReceiveDefault = preViaReceiveDefaults.toArray(new Consumer[preViaReceiveDefaults.size()]);
        this.preViaReceiveHandlers = preViaReceiveHandlers.mapValuesToRawValues(value -> value == null ? preViaReceiveDefault : value.toArray(new Consumer[value.size()]));
        Object[] prePredictionReceiveDefault = prePredictionReceiveDefaults.toArray(new Consumer[preViaReceiveDefaults.size()]);
        this.prePredictionReceiveHandlers = prePredictionReceiveHandlers.mapValuesToRawValues(value -> value == null ? prePredictionReceiveDefault : value.toArray(new Consumer[value.size()]));
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
