package ac.grim.grimac.checks.type;

import ac.grim.grimac.api.AbstractCheck;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;

public interface PrePredictionPacketReceiveListener extends AbstractCheck {
    void onPrePredictionPacketReceive(PacketReceiveEvent event);
}
