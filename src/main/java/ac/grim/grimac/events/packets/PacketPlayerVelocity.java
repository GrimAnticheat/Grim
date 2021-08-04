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

            // If the player isn't in a vehicle and the ID is for the player, the player will take kb
            // If the player is in a vehicle and the ID is for the player's vehicle, the player will take kb
            Vector3d playerVelocity = velocity.getVelocity();

            int reservedID = player.getNextTransactionID(2);
            short breadOne = (short) reservedID;
            short breadTwo = (short) (reservedID - 1);

            Entity vehicle = player.bukkitPlayer.getVehicle();
            if (entityId == player.entityID || (vehicle != null && vehicle.getEntityId() == entityId)) {
                // Wrap velocity between two transactions
                player.sendTransactionOrPingPong(breadOne, false);
                player.knockbackHandler.addPlayerKnockback(breadOne, new Vector(playerVelocity.getX(), playerVelocity.getY(), playerVelocity.getZ()));
                event.setPostTask(() -> player.sendTransactionOrPingPong(breadTwo, true));
            } else {
                // This packet is useless
                // Also prevents a knockback false positive when quickly switching vehicles
                event.setCancelled(true);
            }
        }

        if (packetID == PacketType.Play.Server.EXPLOSION) {
            WrappedPacketOutExplosion explosion = new WrappedPacketOutExplosion(event.getNMSPacket());

            Vector3f velocity = explosion.getPlayerVelocity();

            if (velocity.x != 0 || velocity.y != 0 || velocity.z != 0) {
                GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
                if (player == null) return;
                // No matter what, the player cannot take explosion vector in a vehicle
                if (player.vehicle != null) return;

                int reservedID = player.getNextTransactionID(2);
                short breadOne = (short) reservedID;
                short breadTwo = (short) (reservedID - 1);

                player.sendTransactionOrPingPong(breadOne, false);
                player.explosionHandler.addPlayerExplosion(breadOne, velocity);
                event.setPostTask(() -> player.sendTransactionOrPingPong(breadTwo, true));
            }
        }
    }
}
