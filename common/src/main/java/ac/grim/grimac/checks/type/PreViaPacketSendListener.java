package ac.grim.grimac.checks.type;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;

public interface PreViaPacketSendListener {
    void onPreViaPacketSend(PacketSendEvent event);

    default PacketTypeCommon[] sendTypes() {
        return PacketSendListener.NO_SEND_TYPES;
    }
}
