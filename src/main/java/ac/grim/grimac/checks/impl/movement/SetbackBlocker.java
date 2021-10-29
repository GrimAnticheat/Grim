package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.utils.vector.Vector3d;

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
            if (player.inVehicle && event.getPacketId() != PacketType.Play.Client.LOOK && !player.packetStateData.lastPacketWasTeleport) {
                event.setCancelled(true);
            }

            // The player is sleeping, should be safe to block position packets
            if (player.isInBed && new Vector3d(player.x, player.y, player.z).distanceSquared(player.bedPosition) > 1) {
                event.setCancelled(true);
            }

            // Player is dead
            if (player.isDead) {
                event.setCancelled(true);
            }
        }

        if (event.getPacketId() == PacketType.Play.Client.VEHICLE_MOVE) {
            if (player.getSetbackTeleportUtil().shouldBlockMovement()) {
                event.setCancelled(true);
            }

            // Don't let a player move a vehicle when not in a vehicle
            if (!player.inVehicle) {
                event.setCancelled(true);
            }

            // A player is sleeping while in a vehicle
            if (player.isInBed) {
                event.setCancelled(true);
            }

            // Player is dead
            if (player.isDead) {
                event.setCancelled(true);
            }
        }
    }
}
