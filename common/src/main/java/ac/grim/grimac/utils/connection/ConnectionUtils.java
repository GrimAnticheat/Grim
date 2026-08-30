package ac.grim.grimac.utils.connection;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.PacketSide;
import com.github.retrooper.packetevents.util.PacketEventsImplHelper;
import com.github.retrooper.packetevents.wrapper.PacketTypeData;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@UtilityClass
public class ConnectionUtils {

    @SuppressWarnings("UnstableApiUsage")
    public static final String PRE_VIA_DECODER_NAME = "pre-" + PacketEvents.DECODER_NAME;

    @SuppressWarnings("UnstableApiUsage")
    public static final String PRE_VIA_ENCODER_NAME = "pre-" + PacketEvents.ENCODER_NAME;

    public void sendPacketPreVia(@NotNull GrimPlayer player, @NotNull PacketWrapper<?> wrapper) {
        injectPacketPreVia(player, wrapper, PacketSide.SERVER);
    }

    public void sendPacketPreVia(@NotNull GrimPlayer player, @NotNull Object buffer) {
        injectPacketPreVia(player, buffer, PacketSide.SERVER);
    }

    public void receivePacketPreVia(@NotNull GrimPlayer player, @NotNull PacketWrapper<?> wrapper) {
        injectPacketPreVia(player, wrapper, PacketSide.CLIENT);
    }

    public void receivePacketPreVia(@NotNull GrimPlayer player, @NotNull Object buffer) {
        injectPacketPreVia(player, buffer, PacketSide.CLIENT);
    }

    public void injectPacketPreVia(@NotNull GrimPlayer player, @NotNull PacketWrapper<?> wrapper, @NotNull PacketSide side) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(wrapper, "wrapper");
        Objects.requireNonNull(side, "side");

        Object buffer = ChannelHelper.pooledByteBuf(player.user.getChannel());
        @SuppressWarnings("UnstableApiUsage")
        PacketTypeData packetTypeData = wrapper.getPacketTypeData();
        Objects.requireNonNull(packetTypeData, "packetTypeData");

        int packetId = packetTypeData.getPacketType() != null
                ? packetTypeData.getPacketType().getId(player.getClientVersion())
                : packetTypeData.getNativePacketId();
        Object prevBuffer = wrapper.buffer;
        wrapper.buffer = buffer;
        wrapper.writeVarInt(packetId);
        wrapper.write();
        wrapper.buffer = prevBuffer;
        injectPacketPreVia(player, buffer, side);
    }

    public void injectPacketPreVia(@NotNull GrimPlayer player, @NotNull Object buffer, @NotNull PacketSide side) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(buffer, "buffer");
        Objects.requireNonNull(side, "side");

        Object channel = player.user.getChannel();
        Object nativePlayer = player.platformPlayer == null ? null : player.platformPlayer.getNative();

        // call pre-via listeners
        try {
            PacketEventsImplHelper.handlePacket(channel, player.user, nativePlayer, buffer, false, side);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ByteBufHelper.retain(buffer);

        // pass buffer to next handler
        if (side == PacketSide.SERVER) {
            ChannelHelper.writeAndFlushInContext(channel, PRE_VIA_ENCODER_NAME, buffer);
        } else {
            ChannelHelper.fireChannelReadInContext(channel, PRE_VIA_DECODER_NAME, buffer);
        }
    }
}
