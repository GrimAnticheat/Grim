package ac.grim.grimac.checks.type;

import ac.grim.grimac.api.AbstractCheck;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface PacketCheck extends AbstractCheck {
    default Map<PacketTypeCommon, List<Consumer<PacketSendEvent>>> getSendHandlers() {
        return Map.of();
    }

    default Map<PacketTypeCommon, List<Consumer<PacketReceiveEvent>>> getReceiveHandlers() {
        return Map.of();
    }
}
