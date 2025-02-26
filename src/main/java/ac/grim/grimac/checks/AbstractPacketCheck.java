package ac.grim.grimac.checks;

import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Getter
public class AbstractPacketCheck extends Check implements PacketCheck {

    public AbstractPacketCheck(GrimPlayer player) {
        super(player);
    }

    protected void registerSendHandlers(PacketHandlerRegistry<PacketSendEvent> registry) {
    }

    protected void registerReceiveHandlers(PacketHandlerRegistry<PacketReceiveEvent> registry) {
    }

    @Override
    public Map<PacketTypeCommon, List<Consumer<PacketSendEvent>>> getSendHandlers() {
        PacketHandlerRegistry<PacketSendEvent> registry = new PacketHandlerRegistry<>(false);
        registerSendHandlers(registry);
        return registry.getHandlers();
    }

    @Override
    public Map<PacketTypeCommon, List<Consumer<PacketReceiveEvent>>> getReceiveHandlers() {
        PacketHandlerRegistry<PacketReceiveEvent> registry = new PacketHandlerRegistry<>(true);
        registerReceiveHandlers(registry);
        return registry.getHandlers();
    }
}
