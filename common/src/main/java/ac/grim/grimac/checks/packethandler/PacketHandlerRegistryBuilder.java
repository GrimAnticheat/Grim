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
    private final IncompleteHandlerMap<PacketSendEvent> sendHandlers = new IncompleteHandlerMap<>(PacketSide.SERVER);
    private final IncompleteHandlerMap<PacketSendEvent> preViaSendHandlers = new IncompleteHandlerMap<>(PacketSide.SERVER);
    private final IncompleteHandlerMap<PacketReceiveEvent> receiveHandlers = new IncompleteHandlerMap<>(PacketSide.CLIENT);
    private final IncompleteHandlerMap<PacketReceiveEvent> preViaReceiveHandlers = new IncompleteHandlerMap<>(PacketSide.CLIENT);
    private final IncompleteHandlerMap<PacketReceiveEvent> prePredictionReceiveHandlers = new IncompleteHandlerMap<>(PacketSide.CLIENT);

    public void registerSendHandler(@NotNull Consumer<@NotNull PacketSendEvent> handler, @NotNull ClientBoundPacket @NotNull ... types) {
        sendHandlers.put(handler, types);
    }

    public void registerPreViaSendHandler(@NotNull Consumer<@NotNull PacketSendEvent> handler, @NotNull ClientBoundPacket @NotNull ... types) {
        preViaSendHandlers.put(handler, types);
    }

    public void registerReceiveHandler(@NotNull Consumer<@NotNull PacketReceiveEvent> handler, @NotNull ServerBoundPacket @NotNull ... types) {
        receiveHandlers.put(handler, types);
    }

    public void registerPreViaReceiveHandler(@NotNull Consumer<@NotNull PacketReceiveEvent> handler, @NotNull ServerBoundPacket @NotNull ... types) {
        preViaReceiveHandlers.put(handler, types);
    }

    public void registerPrePredictionReceiveHandler(@NotNull Consumer<@NotNull PacketReceiveEvent> handler, @NotNull ServerBoundPacket @NotNull ... types) {
        prePredictionReceiveHandlers.put(handler, types);
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
                prePredictionReceiveHandlers
        );
    }

    record IncompleteHandlerMap<T extends @NotNull ProtocolPacketEvent>(
            @NotNull PacketMap<ArrayList<@NotNull Consumer<T>>> handlers,
            @NotNull ArrayList<@NotNull Consumer<T>> catchAll
    ) {
        public IncompleteHandlerMap(PacketSide side) {
            this(new PacketMap<>(side), new ArrayList<>());
        }

        public <P> void put(@NotNull Consumer<T> handler, @NotNull P @NotNull [] types) {
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
                put((PacketTypeCommon) type, handler);
            }
        }

        private void put(@NotNull PacketTypeCommon type, @NotNull Consumer<T> handler) {
            handlers.putIfNull(type, () -> new ArrayList<>(catchAll)).add(handler);
        }

        @SuppressWarnings("ToArrayCallWithZeroLengthArrayArgument")
        public @NotNull PacketMap<@NotNull Consumer<T>[]> complete() {
            Object[] sendDefault = catchAll.toArray(new Consumer[catchAll.size()]);
            return handlers.mapValuesToRawValues(value -> value == null ? sendDefault : value.toArray(new Consumer[value.size()]));
        }
    }
}
