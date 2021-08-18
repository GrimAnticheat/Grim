package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.packettype.PacketType;

public class SetbackBlocker extends PacketCheck {
    public SetbackBlocker(GrimPlayer playerData) {
        super(playerData);
    }

    public void onPacketReceive(final PacketPlayReceiveEvent event) {
        if (PacketType.Play.Client.Util.isInstanceOfFlying(event.getPacketId())) {
            // The player must obey setbacks
            if (player.teleportUtil.shouldBlockMovement())
                event.setCancelled(true);
        }

        if (event.getPacketId() == PacketType.Play.Client.VEHICLE_MOVE) {
            if (player.teleportUtil.shouldBlockMovement())
                event.setCancelled(true);
        }
    }
}
