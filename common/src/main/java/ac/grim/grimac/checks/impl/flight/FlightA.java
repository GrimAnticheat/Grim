package ac.grim.grimac.checks.impl.flight;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import ac.grim.grimac.checks.type.PacketListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import org.jetbrains.annotations.NotNull;

// This check catches 100% of cheaters.
public class FlightA extends Check implements PacketListener {
    public FlightA(GrimPlayer player) {
        super(player);
    }

    @Override
    public void registerPacketHandlers(@NotNull PacketHandlerRegistryBuilder registry) {
        registry.registerReceiveHandler(event -> {
            // If the player sends a flying packet, but they aren't flying, then they are cheating.
            if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType()) && !player.isFlying) {
                flag();
            }
        });
    }
}
