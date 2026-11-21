package ac.grim.grimac.checks.impl.timer;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import ac.grim.grimac.checks.type.PacketListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "TickTimer", stableKey = "grim.timer.tick", description = "Did not send client tick end packet", setback = 1)
public class TickTimer extends Check implements PacketListener {
    private static final Verbose V = Verbose.of("type=[end|flying], packets={uint}");

    private boolean receivedTickEnd = true;
    private int flyingPackets = 0;

    public TickTimer(GrimPlayer player) {
        super(player);
    }

    @Override
    public boolean isApplicable() {
        return player.supportsEndTick();
    }

    @Override
    public void registerPacketHandlers(@NotNull PacketHandlerRegistryBuilder registry) {
        registry.registerPrePredictionReceiveHandler(event -> {
            if (player.packetStateData.lastPacketWasTeleport) return;
            if (!receivedTickEnd && flagWithSetback(V.write(verbose()).bool(false).uint(flyingPackets))) {
                handleViolation();
            }
            receivedTickEnd = false;
            flyingPackets++;
        }, FLYING_PACKETS);

        registry.registerPrePredictionReceiveHandler(event -> {
            receivedTickEnd = true;
            if (flyingPackets > 1 && flagWithSetback(V.write(verbose()).bool(true).uint(flyingPackets))) {
                handleViolation();
            }
            flyingPackets = 0;
        }, PacketType.Play.Client.CLIENT_TICK_END);
    }

    private void handleViolation() {
        // Although we don't cancel the packet, this should be counted as an invalid packet.
        player.onPacketCancel();
    }
}
