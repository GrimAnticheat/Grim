package ac.grim.grimac.utils.message;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import net.kyori.adventure.text.Component;

/** A play-state disconnect using the same component encoder as system messages. */
public final class DisconnectMessagePacket extends PacketWrapper<DisconnectMessagePacket> {
    private final int dataVersion;
    private final Component reason;

    public DisconnectMessagePacket(int dataVersion, Component reason) {
        super(PacketType.Play.Server.DISCONNECT);
        this.dataVersion = dataVersion;
        this.reason = reason;
    }

    @Override
    public void write() {
        MessageWireEncoder.write(this, dataVersion, reason);
    }
}
