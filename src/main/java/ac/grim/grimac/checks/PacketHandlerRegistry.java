package ac.grim.grimac.checks;

import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Getter
public class PacketHandlerRegistry<T extends ProtocolPacketEvent> {
    private final Map<PacketTypeCommon, List<Consumer<T>>> handlers = new HashMap<>();

    public void registerHandler(Consumer<T> consumer, PacketTypeCommon... types) {
        if (types.length == 0) {
            registerHandler(consumer, type -> true);
            return;
        }
        for (PacketTypeCommon type : types) {
            handlers.computeIfAbsent(type, __ -> new ArrayList<>()).add(consumer);
        }
    }

    public void registerHandler(Consumer<T> consumer, Predicate<PacketTypeCommon> typePredicate) {
        List<PacketTypeCommon> types = new ArrayList<>();
        for (PacketType.Play.Server packet : PacketType.Play.Server.values()) {
            if (typePredicate.test(packet)) types.add(packet);
        }
        for (PacketType.Login.Server packet : PacketType.Login.Server.values()) {
            if (typePredicate.test(packet)) types.add(packet);
        }
        for (PacketType.Status.Server packet : PacketType.Status.Server.values()) {
            if (typePredicate.test(packet)) types.add(packet);
        }
        for (PacketType.Configuration.Server packet : PacketType.Configuration.Server.values()) {
            if (typePredicate.test(packet)) types.add(packet);
        }
        for (PacketType.Handshaking.Server packet : PacketType.Handshaking.Server.values()) {
            if (typePredicate.test(packet)) types.add(packet);
        }
        registerHandler(consumer, types.toArray(PacketTypeCommon[]::new));
    }
}
