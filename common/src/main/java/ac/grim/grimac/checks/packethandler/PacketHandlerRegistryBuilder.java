package ac.grim.grimac.checks.packethandler;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.PacketSide;
import com.github.retrooper.packetevents.protocol.packettype.ClientBoundPacket;
import com.github.retrooper.packetevents.protocol.packettype.ServerBoundPacket;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

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
}
