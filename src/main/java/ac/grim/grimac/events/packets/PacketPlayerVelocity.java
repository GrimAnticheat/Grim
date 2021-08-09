package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.entityvelocity.WrappedPacketOutEntityVelocity;
import io.github.retrooper.packetevents.packetwrappers.play.out.explosion.WrappedPacketOutExplosion;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import io.github.retrooper.packetevents.utils.vector.Vector3f;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

public class PacketPlayerVelocity extends PacketListenerAbstract {

    public PacketPlayerVelocity() {
        super(PacketListenerPriority.MONITOR);
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Server.ENTITY_VELOCITY) {
            WrappedPacketOutEntityVelocity velocity = new WrappedPacketOutEntityVelocity(event.getNMSPacket());
            int entityId = velocity.getEntityId();

            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            Entity playerVehicle = player.bukkitPlayer.getVehicle();

            // Useless velocity packet, cancel to save bandwidth, transactions, and grim processing power
            if ((playerVehicle == null && entityId != player.entityID) || (playerVehicle != null && entityId != playerVehicle.getEntityId())) {
                event.setCancelled(true);
                return;
            }

            // If the player isn't in a vehicle and the ID is for the player, the player will take kb
            // If the player is in a vehicle and the ID is for the player's vehicle, the player will take kb
            Vector3d playerVelocity = velocity.getVelocity();

            // Wrap velocity between two transactions
            player.sendTransactionOrPingPong(player.getNextTransactionID(1), false);
            player.knockbackHandler.addPlayerKnockback(entityId, player.lastTransactionSent.get(), new Vector(playerVelocity.getX(), playerVelocity.getY(), playerVelocity.getZ()));
            event.setPostTask(player::sendAndFlushTransactionOrPingPong);
        }

        if (packetID == PacketType.Play.Server.EXPLOSION) {
            WrappedPacketOutExplosion explosion = new WrappedPacketOutExplosion(event.getNMSPacket());

            Vector3f velocity = explosion.getPlayerVelocity();

            if (velocity.x != 0 || velocity.y != 0 || velocity.z != 0) {
                GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
                if (player == null) return;

                player.sendTransactionOrPingPong(player.getNextTransactionID(1), false);
                player.explosionHandler.addPlayerExplosion(player.lastTransactionSent.get(), velocity);
                event.setPostTask(player::sendAndFlushTransactionOrPingPong);
            }
        }
    }
}
