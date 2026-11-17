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
    private final PacketMap<ArrayList<Consumer<PacketSendEvent>>> sendHandlers = new PacketMap<>(PacketSide.SERVER);
    private final PacketMap<ArrayList<Consumer<PacketSendEvent>>> preViaSendHandlers = new PacketMap<>(PacketSide.SERVER);
    private final PacketMap<ArrayList<Consumer<PacketReceiveEvent>>> receiveHandlers = new PacketMap<>(PacketSide.CLIENT);
    private final PacketMap<ArrayList<Consumer<PacketReceiveEvent>>> preViaReceiveHandlers = new PacketMap<>(PacketSide.CLIENT);
    private final ArrayList<Consumer<PacketSendEvent>> sendCatchAll = new ArrayList<>();
    private final ArrayList<Consumer<PacketSendEvent>> preViaSendCatchAll = new ArrayList<>();
    private final ArrayList<Consumer<PacketReceiveEvent>> receiveCatchAll = new ArrayList<>();
    private final ArrayList<Consumer<PacketReceiveEvent>> preViaReceiveCatchAll = new ArrayList<>();

    public void registerSendHandler(@NotNull Consumer<@NotNull PacketSendEvent> consumer, @NotNull ClientBoundPacket @NotNull ... types) {
        register(consumer, types, sendHandlers, sendCatchAll);
    }

    public void registerPreViaSendHandler(@NotNull Consumer<@NotNull PacketSendEvent> consumer, @NotNull ClientBoundPacket @NotNull ... types) {
        register(consumer, types, preViaSendHandlers, preViaSendCatchAll);
    }

    public void registerReceiveHandler(@NotNull Consumer<@NotNull PacketReceiveEvent> consumer, @NotNull ServerBoundPacket @NotNull ... types) {
        register(consumer, types, receiveHandlers, receiveCatchAll);
    }

    public void registerPreViaReceiveHandler(@NotNull Consumer<@NotNull PacketReceiveEvent> consumer, @NotNull ServerBoundPacket @NotNull ... types) {
        register(consumer, types, preViaReceiveHandlers, preViaReceiveCatchAll);
    }

    private static <T extends @NotNull ProtocolPacketEvent, P> void register(
            @NotNull Consumer<T> consumer,
            @NotNull P @NotNull [] types,
            PacketMap<ArrayList<Consumer<T>>> handlers,
            ArrayList<Consumer<T>> catchAll
    ) {
        if (types.length == 0) {
            catchAll.add(consumer);
            // A catchAll registered before a typed handler has to run before it.
            handlers.forEachValue(value -> {
                if (value != null) {
                    value.add(consumer);
                }
            });
            return;
        }

        for (@NotNull P type : types) {
            handlers.putIfNull((PacketTypeCommon) type, () -> new ArrayList<>(catchAll)).add(consumer);
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
