package ac.grim.grimac.checks.type;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;

public interface PrePredictionPacketReceiveListener {
    void onPrePredictionPacketReceive(PacketReceiveEvent event);
}
