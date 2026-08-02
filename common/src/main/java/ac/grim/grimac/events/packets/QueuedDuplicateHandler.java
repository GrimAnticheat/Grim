package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.QueuedDuplicate;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.PacketEventsImplHelper;
import org.jetbrains.annotations.NotNull;

public class QueuedDuplicateHandler extends PacketListenerAbstract {

    private static final String PRE_VIA_DECODER_NAME = "pre-" + PacketEvents.DECODER_NAME;

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

        // PacketEvents doesn't have the proper API for this; the packet id,
        // which we can't change, is always set using the server protocol.
        // As a bonus of doing it this way, we allocate fewer objects.
        Object channel = player.user.getChannel();
        Object buffer = ChannelHelper.pooledByteBuf(channel);

        int packetId = PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION
                .getId(event.getServerVersion().toClientVersion());
        ByteBufHelper.writeVarInt(buffer, packetId);
        ByteBufHelper.writeDouble(buffer, queuedDuplicate.x());
        ByteBufHelper.writeDouble(buffer, queuedDuplicate.y());
        ByteBufHelper.writeDouble(buffer, queuedDuplicate.z());
        ByteBufHelper.writeFloat(buffer, queuedDuplicate.yaw());
        ByteBufHelper.writeFloat(buffer, queuedDuplicate.pitch());
        ByteBufHelper.writeBoolean(buffer, queuedDuplicate.onGround());

        // call pre-via listeners
        try {
            PacketEventsImplHelper.handleServerBoundPacket(channel, player.user, event.getPlayer(), buffer, false);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ByteBufHelper.retain(buffer);

        // pass buffer to next handler
        ChannelHelper.fireChannelReadInContext(channel, PRE_VIA_DECODER_NAME, buffer);

        player.packetStateData.queuedDuplicate = null;
        player.packetStateData.lastPacketWasOnePointSeventeenDuplicate = player.packetStateData.isReceivingQueuedDuplicate = false;
    }
}
