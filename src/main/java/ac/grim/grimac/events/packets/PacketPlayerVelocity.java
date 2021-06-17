package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.event.priority.PacketEventPriority;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.entityvelocity.WrappedPacketOutEntityVelocity;
import io.github.retrooper.packetevents.packetwrappers.play.out.explosion.WrappedPacketOutExplosion;
import io.github.retrooper.packetevents.packetwrappers.play.out.transaction.WrappedPacketOutTransaction;
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

            if (entityId == player.entityID || (player.packetStateData.vehicle != null && player.packetStateData.vehicle == entityId)) {
                double velX = velocity.getVelocityX();
                double velY = velocity.getVelocityY();
                double velZ = velocity.getVelocityZ();

                Vector playerVelocity = new Vector(velX, velY, velZ);

                int reservedID = (-1 * (player.lastTransactionSent.getAndAdd(2) % 32768));
                short breadOne = (short) reservedID;
                short breadTwo = (short) (reservedID - 1);

                // Wrap velocity between two transactions
                PacketEvents.get().getPlayerUtils().sendPacket(player.bukkitPlayer, new WrappedPacketOutTransaction(0, breadOne, false));
                player.knockbackHandler.addPlayerKnockback(breadOne, playerVelocity);
                event.setPostTask(() -> PacketEvents.get().getPlayerUtils().sendPacket(player.bukkitPlayer, new WrappedPacketOutTransaction(0, breadTwo, false)));
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

                int reservedID = (-1 * (player.lastTransactionSent.getAndAdd(2) % 32768));
                short breadOne = (short) reservedID;
                short breadTwo = (short) (reservedID - 1);

                PacketEvents.get().getPlayerUtils().sendPacket(player.bukkitPlayer, new WrappedPacketOutTransaction(0, breadOne, false));
                player.explosionHandler.addPlayerExplosion(breadOne, explosion);
                event.setPostTask(() -> PacketEvents.get().getPlayerUtils().sendPacket(player.bukkitPlayer, new WrappedPacketOutTransaction(0, breadTwo, false)));
            }
        }
    }
}
