package ac.grim.grimac.checks.packethandler;

import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.PacketSide;
import com.github.retrooper.packetevents.protocol.packettype.ClientBoundPacket;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.packettype.ServerBoundPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class PacketMap<T> {

    private static final PacketType.Configuration.Client[] CLIENT_CONFIGURATION = PacketType.Configuration.Client.values();
    private static final PacketType.Play.Client[] CLIENT_PLAY = PacketType.Play.Client.values();
    private static final PacketType.Configuration.Server[] SERVER_CONFIGURATION = PacketType.Configuration.Server.values();
    private static final PacketType.Play.Server[] SERVER_PLAY = PacketType.Play.Server.values();

    private static final int CLIENT_CONFIGURATION_LENGTH = CLIENT_CONFIGURATION.length;
    private static final int CLIENT_PLAY_LENGTH = CLIENT_PLAY.length;
    private static final int SERVER_CONFIGURATION_LENGTH = SERVER_CONFIGURATION.length;
    private static final int SERVER_PLAY_LENGTH = SERVER_PLAY.length;

    private final Object[] configuration, play;

    public PacketMap(@NotNull PacketSide side) {
        boolean isServer = side == PacketSide.SERVER;
        this.configuration = new Object[isServer ? SERVER_CONFIGURATION_LENGTH : CLIENT_CONFIGURATION_LENGTH];
        this.play = new Object[isServer ? SERVER_PLAY_LENGTH : CLIENT_PLAY_LENGTH];
    }

    public void put(@NotNull ServerBoundPacket packetType, @NotNull T value) {
        if (packetType instanceof PacketType.Configuration.Client p) {
            configuration[p.ordinal()] = value;
        } else if (packetType instanceof PacketType.Play.Client p) {
            play[p.ordinal()] = value;
        } else {
            throw new IllegalArgumentException("unknown client packet: " + packetType);
        }
    }

    public void put(@NotNull ClientBoundPacket packetType, @NotNull T value) {
        if (packetType instanceof PacketType.Configuration.Server p) {
            configuration[p.ordinal()] = value;
        } else if (packetType instanceof PacketType.Play.Server p) {
            play[p.ordinal()] = value;
        } else {
            throw new IllegalArgumentException("unknown server packet: " + packetType);
        }
    }

    public T putIfNull(@NotNull PacketTypeCommon packetType, @NotNull Supplier<T> newValue) {
        if (packetType instanceof PacketType.Configuration.Client p) {
            return putIfNull(p.ordinal(), configuration, newValue);
        } else if (packetType instanceof PacketType.Play.Client p) {
            return putIfNull(p.ordinal(), play, newValue);
        } else if (packetType instanceof PacketType.Configuration.Server p) {
            return putIfNull(p.ordinal(), configuration, newValue);
        } else if (packetType instanceof PacketType.Play.Server p) {
            return putIfNull(p.ordinal(), play, newValue);
        } else {
            return newValue.get();
        }
    }

    private static <T> T putIfNull(int idx, Object @NotNull [] array, @NotNull Supplier<T> newValue) {
        @SuppressWarnings("unchecked")
        T inArray = (T) array[idx];
        if (inArray == null) {
            T value = newValue.get();
            array[idx] = value;
            return value;
        }

        return inArray;
    }

    @SuppressWarnings("unchecked")
    public @Nullable T get(PacketTypeCommon packetType, @NotNull ConnectionState connectionState) {
        return switch (connectionState) {
            case CONFIGURATION -> (T) configuration[((Enum<?>) packetType).ordinal()];
            case PLAY -> (T) play[((Enum<?>) packetType).ordinal()];
            default -> null;
        };
    }

    @SuppressWarnings("unchecked")
    public void forEachValue(Consumer<T> function) {
        for (Object value : this.configuration) {
            function.accept((T) value);
        }

        for (Object value : this.play) {
            function.accept((T) value);
        }
    }

    @SuppressWarnings("unchecked")
    public <V> PacketMap<V> mapValuesToRawValues(Function<@Nullable T, Object> mapper) {
        for (int i = 0; i < this.configuration.length; i++) {
            this.configuration[i] = mapper.apply((T) configuration[i]);
        }

        for (int i = 0; i < this.play.length; i++) {
            this.play[i] = mapper.apply((T) play[i]);
        }

        return (PacketMap<V>) this;
    }
}
