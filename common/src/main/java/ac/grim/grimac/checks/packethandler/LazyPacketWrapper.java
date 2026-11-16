package ac.grim.grimac.checks.packethandler;

import ac.grim.grimac.utils.reflection.ReflectionUtils;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.PacketSide;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;

public class LazyPacketWrapper {
    private static final PacketMap<@Nullable Function<@NotNull ProtocolPacketEvent, ? extends @NotNull PacketWrapper<?>>> clientConstructors
            = PacketMap.populated(PacketSide.CLIENT, LazyPacketWrapper::getConstructor);
    private static final PacketMap<@Nullable Function<@NotNull ProtocolPacketEvent, ? extends @NotNull PacketWrapper<?>>> serverConstructors
            = PacketMap.populated(PacketSide.SERVER, LazyPacketWrapper::getConstructor);

    private @Nullable PacketWrapper<?> wrapper;
    private final ProtocolPacketEvent event;
    private final @Nullable Function<ProtocolPacketEvent, ? extends PacketWrapper<?>> ctor;

    public LazyPacketWrapper(@NotNull ProtocolPacketEvent event) {
        this.wrapper = event.getLastUsedWrapper();
        this.event = event;

        if (this.wrapper != null) {
            // skip constructor, it's not needed
            this.ctor = null;
            return;
        }

        PacketTypeCommon packetType = event.getPacketType();
        ConnectionState connectionState = event.getConnectionState();
        try {
            this.ctor = switch (packetType.getSide()) {
                case CLIENT -> clientConstructors.get(packetType, connectionState);
                case SERVER -> serverConstructors.get(packetType, connectionState);
            };
        } catch (Throwable e) {
            throw new RuntimeException("could not get constructor for " + packetType.getName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get() {
        if (this.wrapper != null) {
            return (T) this.wrapper;
        }

        if (this.event.getLastUsedWrapper() != null) {
            this.wrapper = this.event.getLastUsedWrapper();
            return get();
        }

        if (this.ctor != null) {
            this.wrapper = this.ctor.apply(this.event);
            return get();
        }

        throw new IllegalStateException("Unknown wrapper constructor for " + this.event.getPacketType());
    }

    private static <E extends @NotNull ProtocolPacketEvent> @Nullable Function<E, ? extends @NotNull PacketWrapper<?>> getConstructor(final @NotNull PacketTypeCommon packetType) {
        Class<? extends PacketWrapper<?>> wrapperClass = packetType.getWrapperClass();
        if (wrapperClass == null) return null;

        Class<?> eventClass = packetType.getSide() == PacketSide.CLIENT ? PacketReceiveEvent.class : PacketSendEvent.class;
        Constructor<? extends PacketWrapper<?>> constructor = ReflectionUtils.getConstructor(wrapperClass, eventClass);
        if (constructor == null) return null;

        return event -> {
            try {
                return constructor.newInstance(event);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("could not construct wrapper for " + packetType.getName() + " (has it already been read?)", e);
            }
        };
    }
}
