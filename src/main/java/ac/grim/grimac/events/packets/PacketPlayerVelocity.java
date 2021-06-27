package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.event.priority.PacketEventPriority;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.entityvelocity.WrappedPacketOutEntityVelocity;
import io.github.retrooper.packetevents.packetwrappers.play.out.explosion.WrappedPacketOutExplosion;
import org.bukkit.util.Vector;

public class PacketPlayerVelocity extends PacketListenerAbstract {
    public PacketPlayerVelocity() {
        super(PacketEventPriority.HIGHEST);
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
            if ((player.packetStateData.vehicle == null && entityId == player.entityID) || (player.packetStateData.vehicle != null && player.packetStateData.vehicle == entityId)) {
                double velX = velocity.getVelocityX();
                double velY = velocity.getVelocityY();
                double velZ = velocity.getVelocityZ();

                Vector playerVelocity = new Vector(velX, velY, velZ);

                int reservedID = (-1 * (player.lastTransactionSent.getAndAdd(2) % 32768));
                short breadOne = (short) reservedID;
                short breadTwo = (short) (reservedID - 1);

                // Wrap velocity between two transactions
                player.sendTransactionOrPingPong(breadOne);
                player.knockbackHandler.addPlayerKnockback(breadOne, playerVelocity);
                event.setPostTask(() -> player.sendTransactionOrPingPong(breadTwo));
            }
        }

        if (packetID == PacketType.Play.Server.EXPLOSION) {
            WrappedPacketOutExplosion explosion = new WrappedPacketOutExplosion(event.getNMSPacket());

            double x = explosion.getPlayerMotionX();
            double y = explosion.getPlayerMotionY();
            double z = explosion.getPlayerMotionZ();

            if (x != 0 || y != 0 || z != 0) {
                GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
                if (player == null) return;
                // No matter what, the player cannot take explosion vector in a vehicle
                if (player.packetStateData.vehicle != null) return;

                int reservedID = (-1 * (player.lastTransactionSent.getAndAdd(2) % 32768));
                short breadOne = (short) reservedID;
                short breadTwo = (short) (reservedID - 1);

                player.sendTransactionOrPingPong(breadOne);
                player.explosionHandler.addPlayerExplosion(breadOne, explosion);
                event.setPostTask(() -> player.sendTransactionOrPingPong(breadTwo));
            }
        }
    }
}
