package ac.grim.grimac.checks.impl.flight;

import ac.grim.grimac.checks.AbstractPacketCheck;
import ac.grim.grimac.checks.PacketHandlerRegistry;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;

// This check catches 100% of cheaters.
public class FlightA extends AbstractPacketCheck {
    public FlightA(GrimPlayer player) {
        super(player);
    }

    @Override
    protected void registerReceiveHandlers(PacketHandlerRegistry<PacketReceiveEvent> registry) {
        registry.registerHandler(event -> {
            if (!player.isFlying) {
                flag();
            }
        }, this::isFlying);
    }
}
