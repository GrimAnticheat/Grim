package ac.grim.grimac.checks.packethandler;

import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.PacketSide;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.function.Consumer;

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
