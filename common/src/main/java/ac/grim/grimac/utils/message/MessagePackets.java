package ac.grim.grimac.utils.message;

import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import io.netty.buffer.ByteBuf;

public final class MessagePackets {
    private MessagePackets() {
    }

    /** Encoding owns the buffer until transport accepts it, including when a payload is invalid. */
    public static void send(User user, PacketWrapper<?> packet) {
        try {
            packet.prepareForSend(user.getChannel(), true);
        } catch (RuntimeException | Error failure) {
            if (packet.buffer != null) {
                ((ByteBuf) packet.buffer).release();
                packet.buffer = null;
            }
            throw failure;
        }
        Object buffer = packet.buffer;
        packet.buffer = null;
        user.sendPacket(buffer);
    }
}
