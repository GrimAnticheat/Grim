package ac.grim.grimac.checks.type;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;

public interface PacketSendListener {
    PacketTypeCommon[] NO_SEND_TYPES = new PacketTypeCommon[0];

    void onPacketSend(PacketSendEvent event);

    // empty = every send packet
    default PacketTypeCommon[] sendTypes() {
        return NO_SEND_TYPES;
    }
}
