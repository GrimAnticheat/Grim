package ac.grim.grimac.checks.packethandler;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class PacketHandlerRegistry {
    private final PacketMap<PacketHandler<PacketSendEvent>[]> sendHandlers;
    private final PacketMap<PacketHandler<PacketSendEvent>[]> preViaSendHandlers;
    private final PacketMap<PacketHandler<PacketReceiveEvent>[]> receiveHandlers;
    private final PacketMap<PacketHandler<PacketReceiveEvent>[]> preViaReceiveHandlers;

    @SuppressWarnings("ToArrayCallWithZeroLengthArrayArgument")
    PacketHandlerRegistry(
            @NotNull PacketMap<ArrayList<PacketHandler<PacketSendEvent>>> sendHandlers,
            @NotNull PacketMap<ArrayList<PacketHandler<PacketSendEvent>>> preViaSendHandlers,
            @NotNull PacketMap<ArrayList<PacketHandler<PacketReceiveEvent>>> receiveHandlers,
            @NotNull PacketMap<ArrayList<PacketHandler<PacketReceiveEvent>>> preViaReceiveHandlers,
            @NotNull ArrayList<PacketHandler<PacketSendEvent>> sendDefaults,
            @NotNull ArrayList<PacketHandler<PacketSendEvent>> preViaSendDefaults,
            @NotNull ArrayList<PacketHandler<PacketReceiveEvent>> receiveDefaults,
            @NotNull ArrayList<PacketHandler<PacketReceiveEvent>> preViaReceiveDefaults
    ) {
        Object[] sendDefault = sendDefaults.toArray(new PacketHandler[sendDefaults.size()]);
        this.sendHandlers = sendHandlers.mapValuesToRawValues(value -> value == null ? sendDefault : value.toArray(new PacketHandler[value.size()]));
        Object[] preViaSendDefault = preViaSendDefaults.toArray(new PacketHandler[preViaSendDefaults.size()]);
        this.preViaSendHandlers = preViaSendHandlers.mapValuesToRawValues(value -> value == null ? preViaSendDefault : value.toArray(new PacketHandler[value.size()]));
        Object[] receiveDefault = receiveDefaults.toArray(new PacketHandler[receiveDefaults.size()]);
        this.receiveHandlers = receiveHandlers.mapValuesToRawValues(value -> value == null ? receiveDefault : value.toArray(new PacketHandler[value.size()]));
        Object[] preViaReceiveDefault = preViaReceiveDefaults.toArray(new PacketHandler[preViaReceiveDefaults.size()]);
        this.preViaReceiveHandlers = preViaReceiveHandlers.mapValuesToRawValues(value -> value == null ? preViaReceiveDefault : value.toArray(new PacketHandler[value.size()]));
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
            @NotNull PacketMap<@NotNull PacketHandler<E> @NotNull []> handlers
    ) {
        PacketHandler<E>[] array = handlers.get(event.getPacketType(), event.getConnectionState());
        if (array == null) return; // we don't care about this packet

        LazyPacketWrapper wrapper = new LazyPacketWrapper(event);
        for (PacketHandler<E> handler : array) {
            handler.handle(event, wrapper);
        }
    }
}
