package ac.grim.grimac.checks.packethandler;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.PacketSide;
import com.github.retrooper.packetevents.protocol.packettype.ClientBoundPacket;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.packettype.ServerBoundPacket;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.function.Consumer;

public class PacketHandlerRegistryBuilder {
    private final PacketMap<ArrayList<PacketHandler<PacketSendEvent>>> sendHandlers = new PacketMap<>(PacketSide.SERVER);
    private final PacketMap<ArrayList<PacketHandler<PacketSendEvent>>> preViaSendHandlers = new PacketMap<>(PacketSide.SERVER);
    private final PacketMap<ArrayList<PacketHandler<PacketReceiveEvent>>> receiveHandlers = new PacketMap<>(PacketSide.CLIENT);
    private final PacketMap<ArrayList<PacketHandler<PacketReceiveEvent>>> preViaReceiveHandlers = new PacketMap<>(PacketSide.CLIENT);
    private final ArrayList<PacketHandler<PacketSendEvent>> sendCatchAll = new ArrayList<>();
    private final ArrayList<PacketHandler<PacketSendEvent>> preViaSendCatchAll = new ArrayList<>();
    private final ArrayList<PacketHandler<PacketReceiveEvent>> receiveCatchAll = new ArrayList<>();
    private final ArrayList<PacketHandler<PacketReceiveEvent>> preViaReceiveCatchAll = new ArrayList<>();

    public void registerSendHandler(
            @NotNull PacketHandler<@NotNull PacketSendEvent> handler,
            @NotNull ClientBoundPacket @NotNull ... types) {
        register(handler, types, sendHandlers, sendCatchAll);
    }

    public void registerSendHandler(
            @NotNull Consumer<@NotNull PacketSendEvent> handler,
            @NotNull ClientBoundPacket @NotNull ... types) {
        registerSendHandler((event, wrapper) -> handler.accept(event), types);
    }

    public void registerPreViaSendHandler(
            @NotNull PacketHandler<@NotNull PacketSendEvent> handler,
            @NotNull ClientBoundPacket @NotNull ... types) {
        register(handler, types, preViaSendHandlers, preViaSendCatchAll);
    }

    public void registerPreViaSendHandler(
            @NotNull Consumer<@NotNull PacketSendEvent> handler,
            @NotNull ClientBoundPacket @NotNull ... types) {
        registerPreViaSendHandler((event, wrapper) -> handler.accept(event), types);
    }

    public void registerReceiveHandler(
            @NotNull PacketHandler<@NotNull PacketReceiveEvent> handler,
            @NotNull ServerBoundPacket @NotNull ... types) {
        register(handler, types, receiveHandlers, receiveCatchAll);
    }

    public void registerReceiveHandler(
            @NotNull Consumer<@NotNull PacketReceiveEvent> handler,
            @NotNull ServerBoundPacket @NotNull ... types) {
        registerReceiveHandler((event, wrapper) -> handler.accept(event), types);
    }

    public void registerPreViaReceiveHandler(
            @NotNull PacketHandler<@NotNull PacketReceiveEvent> handler,
            @NotNull ServerBoundPacket @NotNull ... types) {
        register(handler, types, preViaReceiveHandlers, preViaReceiveCatchAll);
    }

    public void registerPreViaReceiveHandler(
            @NotNull Consumer<@NotNull PacketReceiveEvent> handler,
            @NotNull ServerBoundPacket @NotNull ... types) {
        registerPreViaReceiveHandler((event, wrapper) -> handler.accept(event), types);
    }

    private static <E extends @NotNull ProtocolPacketEvent, P> void register(
            @NotNull PacketHandler<E> handler,
            @NotNull P @NotNull [] types,
            PacketMap<ArrayList<PacketHandler<E>>> handlers,
            ArrayList<PacketHandler<E>> catchAll
    ) {
        if (types.length == 0) {
            catchAll.add(handler);
            // A catchAll registered before a typed handler has to run before it.
            handlers.forEachValue(value -> {
                if (value != null) {
                    value.add(handler);
                }
            });
            return;
        }

        for (@NotNull P type : types) {
            handlers.putIfNull((PacketTypeCommon) type, () -> new ArrayList<>(catchAll)).add(handler);
        }
    }

    /**
     * Creates and returns a {@link PacketHandlerRegistry} from this builder.
     * This builder object must not be used after calling this method.
     */
    @Contract(value = "->new", pure = true)
    public @NotNull PacketHandlerRegistry build() {
        return new PacketHandlerRegistry(
                sendHandlers,
                preViaSendHandlers,
                receiveHandlers,
                preViaReceiveHandlers,
                sendCatchAll,
                preViaSendCatchAll,
                receiveCatchAll,
                preViaReceiveCatchAll
        );
    }
}
