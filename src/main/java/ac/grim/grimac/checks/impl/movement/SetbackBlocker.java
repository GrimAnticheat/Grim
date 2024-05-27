package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

public class SetbackBlocker extends Check implements PacketCheck {
    public SetbackBlocker(final GrimPlayer playerData) {
        super(playerData);
    }

    public void onPacketReceive(final PacketReceiveEvent event) {
        if (player.disableGrim) return; // Let's avoid letting people disable grim with grim.nomodifypackets

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            if (player.getSetbackTeleportUtil().cheatVehicleInterpolationDelay > 0) {
                event.setCancelled(true); // Player is in the vehicle
            }
        }

        // Don't block teleport packets
        if (player.packetStateData.lastPacketWasTeleport) return;

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            event.setCancelled(player.getSetbackTeleportUtil().shouldBlockMovement() ||
                    (player.compensatedEntities.getSelf().inVehicle() && event.getPacketType() != PacketType.Play.Client.PLAYER_ROTATION && !player.packetStateData.lastPacketWasTeleport) ||
                    player.isInBed && new Vector3d(player.x, player.y, player.z).distanceSquared(player.bedPosition) > 1 ||
                    player.compensatedEntities.getSelf().isDead);
        } else if (event.getPacketType() == PacketType.Play.Client.VEHICLE_MOVE) {
            event.setCancelled(player.getSetbackTeleportUtil().shouldBlockMovement() ||
                    !player.compensatedEntities.getSelf().inVehicle() ||
                    player.isInBed ||
                    player.compensatedEntities.getSelf().isDead);
        }
    }
}
