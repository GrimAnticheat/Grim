package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.checks.GrimProcessor;
import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import ac.grim.grimac.checks.type.PacketListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.VectorUtils;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import org.jetbrains.annotations.NotNull;

public class SetbackBlocker extends GrimProcessor implements PacketListener {
    public SetbackBlocker(GrimPlayer player) {
        super(player);
    }

    @Override
    public void registerPacketHandlers(@NotNull PacketHandlerRegistryBuilder registry) {
        registry.registerReceiveHandler(event -> {
            if (player.disableGrim)
                return; // Let's avoid letting people disable grim with grim.nomodifypackets

            if (player.getSetbackTeleportUtil().cheatVehicleInterpolationDelay > 0) {
                event.setCancelled(true); // Player is in the vehicle
            }
        }, PacketType.Play.Client.INTERACT_ENTITY, PacketType.Play.Client.ATTACK, PacketType.Play.Client.SPECTATE_ENTITY);

        registry.registerReceiveHandler(event -> {
            if (player.disableGrim)
                return; // Let's avoid letting people disable grim with grim.nomodifypackets

            // Don't block teleport packets
            if (player.packetStateData.lastPacketWasTeleport) return;

            // The player must obey setbacks
            if (player.getSetbackTeleportUtil().shouldBlockMovement()) {
                event.setCancelled(true);
            }

            // Look is the only valid packet to send while in a vehicle
            if (player.inVehicle() && event.getPacketType() != PacketType.Play.Client.PLAYER_ROTATION && !player.packetStateData.lastPacketWasTeleport) {
                event.setCancelled(true);
            }

            // The player is sleeping, should be safe to block position packets
            if (player.isInBed && player.bedPosition != null && VectorUtils.distanceSquared(player.x, player.y, player.z, player.bedPosition) > 1) {
                event.setCancelled(true);
            }

            // Player is dead
            if (player.compensatedEntities.self.isDead) {
                event.setCancelled(true);
            }
        }, FLYING_PACKETS);

        registry.registerReceiveHandler(event -> {
            if (player.disableGrim)
                return; // Let's avoid letting people disable grim with grim.nomodifypackets

            // Don't block teleport packets
            if (player.packetStateData.lastPacketWasTeleport) return;

            if (player.getSetbackTeleportUtil().shouldBlockMovement()) {
                event.setCancelled(true);
            }

            // Don't let a player move a vehicle when not in a vehicle
            if (!player.inVehicle()) {
                event.setCancelled(true);
            }

            // A player is sleeping while in a vehicle
            if (player.isInBed) {
                event.setCancelled(true);
            }

            // Player is dead
            if (player.compensatedEntities.self.isDead) {
                event.setCancelled(true);
            }
        }, PacketType.Play.Client.VEHICLE_MOVE);
    }
}
