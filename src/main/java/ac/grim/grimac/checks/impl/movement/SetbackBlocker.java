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
        // Don't block teleport packets
        if (player.packetStateData.lastPacketWasTeleport) return;

        if (PacketType.Play.Client.Util.isInstanceOfFlying(event.getPacketId())) {
            // The player must obey setbacks
            if (player.getSetbackTeleportUtil().shouldBlockMovement()) {
                event.setCancelled(true);
            }

            // Look is the only valid packet to send while in a vehicle
            if (player.packetStateData.isInVehicle && event.getPacketId() != PacketType.Play.Client.LOOK) {
                event.setCancelled(true);
            }

            // The player is sleeping, should be safe to block position packets
            if (player.packetStateData.isInBed && player.packetStateData.packetPosition.distanceSquared(player.packetStateData.bedPosition) > 1) {
                event.setCancelled(true);
            }
        }

        if (event.getPacketId() == PacketType.Play.Client.VEHICLE_MOVE) {
            if (player.getSetbackTeleportUtil().shouldBlockMovement()) {
                event.setCancelled(true);
            }

            // Don't let a player move a vehicle when not in a vehicle
            if (!player.packetStateData.isInVehicle) {
                event.setCancelled(true);
            }

            // A player is sleeping while in a vehicle
            if (player.packetStateData.isInBed) {
                event.setCancelled(true);
            }
        }
    }
}
