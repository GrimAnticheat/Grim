package ac.grim.grimac.utils.message;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import net.kyori.adventure.text.Component;

public final class MessageWireEncoder {
    private MessageWireEncoder() {
    }

    static void write(PacketWrapper<?> packet, int dataVersion, Component message) {
        write(packet, dataVersion, message, PacketEvents.getAPI().getSettings().shouldDownsampleColors());
    }

    public static void write(PacketWrapper<?> packet, int dataVersion, Component message, boolean downsample) {
        if (packet.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_20_3)) {
            packet.writeNBTRaw(new MessageNbtEncoder(packet, downsample).encode(message));
        } else {
            packet.writeString(KyoriMessageSerializer.forDataVersion(dataVersion, downsample).serialize(message), packet.getMaxMessageLength());
        }
    }
}
