package ac.grim.grimac.checks.type;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PositionUpdate;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;

public abstract class PacketCheck extends Check<Object> {

    public PacketCheck(final GrimPlayer playerData) {
        super(playerData);
    }

    public void onPacketReceive(final PacketReceiveEvent event) {
    }

    public void onPacketSend(final PacketSendEvent event) {
    }

    public void onPositionUpdate(final PositionUpdate positionUpdate) {

    }
}
