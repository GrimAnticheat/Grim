package ac.grim.grimac.checks.packethandler;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PacketHandlerRegistry {
    private final IdentityHashMap<PacketTypeCommon, Consumer<PacketSendEvent>[]> sendHandlers;
    private final IdentityHashMap<PacketTypeCommon, Consumer<PacketSendEvent>[]> preViaSendHandlers;
    private final IdentityHashMap<PacketTypeCommon, Consumer<PacketReceiveEvent>[]> receiveHandlers;
    private final IdentityHashMap<PacketTypeCommon, Consumer<PacketReceiveEvent>[]> preViaReceiveHandlers;
    private final Consumer<PacketSendEvent>[] sendCatchAll;
    private final Consumer<PacketSendEvent>[] preViaSendCatchAll;
    private final Consumer<PacketReceiveEvent>[] receiveCatchAll;
    private final Consumer<PacketReceiveEvent>[] preViaReceiveCatchAll;

    @SuppressWarnings({"ToArrayCallWithZeroLengthArrayArgument", "unchecked"})
    PacketHandlerRegistry(
            @NotNull IdentityHashMap<PacketTypeCommon, ArrayList<Consumer<PacketSendEvent>>> sendHandlers,
            @NotNull IdentityHashMap<PacketTypeCommon, ArrayList<Consumer<PacketSendEvent>>> preViaSendHandlers,
            @NotNull IdentityHashMap<PacketTypeCommon, ArrayList<Consumer<PacketReceiveEvent>>> receiveHandlers,
            @NotNull IdentityHashMap<PacketTypeCommon, ArrayList<Consumer<PacketReceiveEvent>>> preViaReceiveHandlers,
            @NotNull ArrayList<Consumer<PacketSendEvent>> sendCatchAll,
            @NotNull ArrayList<Consumer<PacketSendEvent>> preViaSendCatchAll,
            @NotNull ArrayList<Consumer<PacketReceiveEvent>> receiveCatchAll,
            @NotNull ArrayList<Consumer<PacketReceiveEvent>> preViaReceiveCatchAll
    ) {
        this.sendHandlers = new IdentityHashMap<>(sendHandlers.size());
        this.preViaSendHandlers = new IdentityHashMap<>(preViaSendHandlers.size());
        this.receiveHandlers = new IdentityHashMap<>(receiveHandlers.size());
        this.preViaReceiveHandlers = new IdentityHashMap<>(preViaReceiveHandlers.size());
        this.sendCatchAll = sendCatchAll.toArray(new Consumer[sendCatchAll.size()]);
        this.preViaSendCatchAll = preViaSendCatchAll.toArray(new Consumer[preViaSendCatchAll.size()]);
        this.receiveCatchAll = receiveCatchAll.toArray(new Consumer[receiveCatchAll.size()]);
        this.preViaReceiveCatchAll = preViaReceiveCatchAll.toArray(new Consumer[preViaReceiveCatchAll.size()]);

        for (Map.Entry<PacketTypeCommon, ArrayList<Consumer<PacketSendEvent>>> entry : sendHandlers.entrySet()) {
            ArrayList<Consumer<PacketSendEvent>> value = entry.getValue();
            this.sendHandlers.put(entry.getKey(), value.toArray(new Consumer[value.size()]));
        }

        for (Map.Entry<PacketTypeCommon, ArrayList<Consumer<PacketSendEvent>>> entry : preViaSendHandlers.entrySet()) {
            ArrayList<Consumer<PacketSendEvent>> value = entry.getValue();
            this.preViaSendHandlers.put(entry.getKey(), value.toArray(new Consumer[value.size()]));
        }

        for (Map.Entry<PacketTypeCommon, ArrayList<Consumer<PacketReceiveEvent>>> entry : receiveHandlers.entrySet()) {
            ArrayList<Consumer<PacketReceiveEvent>> value = entry.getValue();
            this.receiveHandlers.put(entry.getKey(), value.toArray(new Consumer[value.size()]));
        }

        for (Map.Entry<PacketTypeCommon, ArrayList<Consumer<PacketReceiveEvent>>> entry : preViaReceiveHandlers.entrySet()) {
            ArrayList<Consumer<PacketReceiveEvent>> value = entry.getValue();
            this.preViaReceiveHandlers.put(entry.getKey(), value.toArray(new Consumer[value.size()]));
        }
    }

    public void handleSend(@NotNull PacketSendEvent event) {
        handle(event, sendHandlers, sendCatchAll);
    }

    public void handlePreViaSend(@NotNull PacketSendEvent event) {
        handle(event, preViaSendHandlers, preViaSendCatchAll);
    }

    public void handleReceive(@NotNull PacketReceiveEvent event) {
        handle(event, receiveHandlers, receiveCatchAll);
    }

    public void handlePreViaReceive(@NotNull PacketReceiveEvent event) {
        handle(event, preViaReceiveHandlers, preViaReceiveCatchAll);
    }

    private <T extends @NotNull ProtocolPacketEvent> void handle(T event, @NotNull IdentityHashMap<PacketTypeCommon, Consumer<T>[]> handlers, Consumer<T>[] catchAll) {
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
