package ac.grim.grimac.checks.impl.timer;

import ac.grim.grimac.checks.AbstractPacketCheck;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.PacketHandlerRegistry;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;


@CheckData(name = "TickTimer", setback = 1)
public class TickTimer extends AbstractPacketCheck implements PostPredictionCheck {

    private boolean receivedTickEnd = true;
    private int flyingPackets = 0;

    public TickTimer(GrimPlayer player) {
        super(player);
    }

    @Override
    protected void registerReceiveHandlers(PacketHandlerRegistry<PacketReceiveEvent> registry) {
        if (!player.supportsEndTick()) return;
        registry.registerHandler(event -> {
            if (player.packetStateData.lastPacketWasTeleport) return;
            if (!receivedTickEnd && flagAndAlertWithSetback("type=flying, packets=" + flyingPackets)) {
                handleViolation();
            }
            receivedTickEnd = false;
            flyingPackets++;
        }, this::isFlying);
        registry.registerHandler(event -> {
            receivedTickEnd = true;
            if (flyingPackets > 1 && flagAndAlertWithSetback("type=end, packets=" + flyingPackets)) {
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
