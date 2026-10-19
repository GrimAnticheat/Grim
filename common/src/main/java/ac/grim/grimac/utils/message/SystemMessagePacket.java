package ac.grim.grimac.utils.message;

import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.text.Component;


/**
 * A server-format system message. PE supplies packet IDs, buffer ownership and transport only;
 * Grim supplies the component encoding. Via translates the packet on its way out.
 */
public final class SystemMessagePacket extends PacketWrapper<SystemMessagePacket> {
    private final Component message;
    private final int dataVersion;

    public SystemMessagePacket(ServerVersion version, int dataVersion, Component message) {
        super(version.isNewerThanOrEquals(ServerVersion.V_1_19)
                ? PacketType.Play.Server.SYSTEM_CHAT_MESSAGE : PacketType.Play.Server.CHAT_MESSAGE);
        this.message = message;
        this.dataVersion = dataVersion;
        setServerVersion(version);
    }

    @Override
    public void write() {
        MessageWireEncoder.write(this, dataVersion, message);
        writeTrailer((ByteBuf) buffer, serverVersion);
    }

    static void writeTrailer(ByteBuf buffer, ServerVersion version) {
        if (version.isNewerThanOrEquals(ServerVersion.V_1_19_1)) {
            buffer.writeBoolean(false); // overlay
        } else if (version.isNewerThanOrEquals(ServerVersion.V_1_19)) {
            buffer.writeByte(1); // VarInt system message type
        } else {
            buffer.writeByte(1); // legacy system chat position
            if (version.isNewerThanOrEquals(ServerVersion.V_1_16)) {
                buffer.writeLong(0).writeLong(0); // no player sender
            }
        }
    }
}
