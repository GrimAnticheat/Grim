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

public class PacketHandlerRegistry<T extends ProtocolPacketEvent> {
    @Getter
    private final Map<PacketTypeCommon, List<Consumer<T>>> handlers = new HashMap<>();
    private final boolean serverbound;

    public PacketHandlerRegistry(boolean serverbound) {
        this.serverbound = serverbound;
    }

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
        if (serverbound) {
            for (PacketType.Play.Client packet : PacketType.Play.Client.values()) {
                if (typePredicate.test(packet)) types.add(packet);
            }
            for (PacketType.Login.Client packet : PacketType.Login.Client.values()) {
                if (typePredicate.test(packet)) types.add(packet);
            }
            for (PacketType.Status.Client packet : PacketType.Status.Client.values()) {
                if (typePredicate.test(packet)) types.add(packet);
            }
            for (PacketType.Configuration.Client packet : PacketType.Configuration.Client.values()) {
                if (typePredicate.test(packet)) types.add(packet);
            }
            for (PacketType.Handshaking.Client packet : PacketType.Handshaking.Client.values()) {
                if (typePredicate.test(packet)) types.add(packet);
            }
        } else {
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
        }
        registerHandler(consumer, types.toArray(PacketTypeCommon[]::new));
    }
}
