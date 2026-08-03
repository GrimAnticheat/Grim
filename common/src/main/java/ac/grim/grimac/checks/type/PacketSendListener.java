package ac.grim.grimac.checks.type;

import ac.grim.grimac.api.AbstractCheck;
import com.github.retrooper.packetevents.event.PacketSendEvent;

public interface PacketSendListener extends AbstractCheck {
    void onPacketSend(PacketSendEvent event);
}
