package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUseBed;

public class BedStateTracker extends PacketListenerAbstract {

    public BedStateTracker() {
        super(PacketListenerPriority.HIGH);
    }

    @Override
    public boolean isPreVia() {
        return true;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.USE_BED) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            WrapperPlayServerUseBed bed = new WrapperPlayServerUseBed(event);
            if (player.entityID != bed.getEntityId()) return;

            Vector3i bedPos = bed.getPosition();
            // Split so packet received after transaction
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                player.isInBed = true;
                player.bedPosition = new Vector3d(bedPos.getX() + 0.5, bedPos.getY(), bedPos.getZ() + 0.5);
            });
        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_ANIMATION) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            WrapperPlayServerEntityAnimation animation = new WrapperPlayServerEntityAnimation(event);
            if (player.entityID == animation.getEntityId() && animation.getType() == WrapperPlayServerEntityAnimation.EntityAnimationType.WAKE_UP) {
                // Split so packet received before transaction
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> player.isInBed = false);
                event.getTasksAfterSend().add(player::sendTransaction);
            }
        }
    }
}
