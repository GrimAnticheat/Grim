package ac.grim.grimac.checks.type;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PositionUpdate;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;

public class PacketCheck extends Check<Object> {

    public PacketCheck(final GrimPlayer playerData) {
        super(playerData);
    }

    public void onPacketReceive(final PacketPlayReceiveEvent event) {
    }

    public void onPacketSend(final PacketPlaySendEvent event) {
    }

    public void onPositionUpdate(final PositionUpdate positionUpdate) {
    }
}
