package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.updatehealth.WrappedPacketOutUpdateHealth;

public class PacketPlayerRespawn extends PacketListenerAbstract {

    public PacketPlayerRespawn() {
        super(PacketListenerPriority.MONITOR);
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Server.UPDATE_HEALTH) {
            WrappedPacketOutUpdateHealth health = new WrappedPacketOutUpdateHealth(event.getNMSPacket());

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            player.sendTransaction();

            if (health.getHealth() <= 0) {
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.isDead = true);
            } else {
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.isDead = false);
            }
        }
    }
}
