package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.player.GrimPlayer;
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

        if (event.getConnectionState() != ConnectionState.PLAY) return;

        if (!Check.isAsync(event.getPacketType()) && player.packetStateData.queuedDuplicate != null && !player.packetStateData.isReceivingQueuedDuplicate) {
            player.packetStateData.isReceivingQueuedDuplicate = true;
            player.packetStateData.lastPacketWasOnePointSeventeenDuplicate = event.getPacketType() == PacketType.Play.Client.USE_ITEM
                    || event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT
                    && new WrapperPlayClientPlayerBlockPlacement(event).getFaceId() == 255
                    && event.getServerVersion().isOlderThan(ServerVersion.V_1_9);

            WrapperPlayClientPlayerFlying packet = player.packetStateData.queuedDuplicate.packet();

            if (player.packetStateData.lastPacketWasOnePointSeventeenDuplicate) {
                CheckManagerListener.handleDuplicate(player, player.packetStateData.queuedDuplicate.event(), packet);
            }

            // we need to make a copy like this or else there's errors (why?!)
            WrapperPlayClientPlayerFlying copy = new WrapperPlayClientPlayerFlying(packet.hasPositionChanged(), packet.hasRotationChanged(), packet.isOnGround(), packet.isHorizontalCollision(), packet.getLocation());
            player.user.receivePacket(copy);

            player.packetStateData.queuedDuplicate = null;
            player.packetStateData.lastPacketWasOnePointSeventeenDuplicate = player.packetStateData.isReceivingQueuedDuplicate = false;
        }
    }
}
