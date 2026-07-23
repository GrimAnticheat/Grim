package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.QueuedDuplicate;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import org.jetbrains.annotations.NotNull;

public class QueuedDuplicateHandler extends PacketListenerAbstract {
    public QueuedDuplicateHandler() {
        super(PacketListenerPriority.LOWEST);
    }

    @Override
    public boolean isPreVia() {
        return true;
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
        if (player == null) return;

        if (event.getConnectionState() != ConnectionState.PLAY
                || Check.isAsync(event.getPacketType())
                || player.packetStateData.isReceivingQueuedDuplicate) return;

        QueuedDuplicate queuedDuplicate = player.packetStateData.queuedDuplicate;
        if (queuedDuplicate == null) return;

        player.packetStateData.isReceivingQueuedDuplicate = true;
        player.packetStateData.lastPacketWasOnePointSeventeenDuplicate = event.getPacketType() == PacketType.Play.Client.USE_ITEM;

        WrapperPlayClientPlayerFlying packet = new WrapperPlayClientPlayerFlying(true, true, queuedDuplicate.onGround(), queuedDuplicate.location());

        if (player.packetStateData.lastPacketWasOnePointSeventeenDuplicate) {
            CheckManagerListener.handleDuplicatePacket(player, queuedDuplicate.event(), packet);
        }

        player.user.receivePacket(packet);

        player.packetStateData.queuedDuplicate = null;
        player.packetStateData.lastPacketWasOnePointSeventeenDuplicate = player.packetStateData.isReceivingQueuedDuplicate = false;
    }
}
