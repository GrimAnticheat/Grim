package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientWindowConfirmation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowConfirmation;
import org.jetbrains.annotations.NotNull;

public class PreViaCheckManagerListener extends PacketListenerAbstract {

    public PreViaCheckManagerListener() {
        super(PacketListenerPriority.LOW);
    }

    @Override
    public boolean isPreVia() {
        return true;
    }

    @Override
    public void onPacketReceive(final @NotNull PacketReceiveEvent event) {
        // Allow checks to listen to configuration packets
        if (event.getConnectionState() == ConnectionState.CONFIGURATION) {
            final GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;
            player.checkManager.onPreViaPacketReceive(event);
        }

        if (event.getConnectionState() != ConnectionState.PLAY) return;
        final GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
        if (player == null) return;

        if (event.getPacketType() == PacketType.Play.Client.WINDOW_CONFIRMATION) {
            WrapperPlayClientWindowConfirmation packet = new WrapperPlayClientWindowConfirmation(event);
            PacketPingListener.onReceiveTransaction(player, event, packet.getActionId());
        } else if (event.getPacketType() == PacketType.Play.Client.PONG) {
            WrapperPlayClientPong packet = new WrapperPlayClientPong(event);
            PacketPingListener.onReceiveTransaction(player, event, packet.getId());
        }

        player.checkManager.onPreViaPacketReceive(event);
    }

    @Override
    public void onPacketSend(final @NotNull PacketSendEvent event) {
        // Allow checks to listen to configuration packets
        if (event.getConnectionState() == ConnectionState.CONFIGURATION) {
            final GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;
            player.checkManager.onPreViaPacketSend(event);
        }

        if (event.getConnectionState() != ConnectionState.PLAY) return;
        final GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
        if (player == null) return;

        if (event.getPacketType() == PacketType.Play.Server.WINDOW_CONFIRMATION) {
            WrapperPlayServerWindowConfirmation packet = new WrapperPlayServerWindowConfirmation(event);
            PacketPingListener.onSendTransaction(player, event, packet.getActionId());
        } else if (event.getPacketType() == PacketType.Play.Server.PING) {
            WrapperPlayServerPing packet = new WrapperPlayServerPing(event);
            PacketPingListener.onSendTransaction(player, event, packet.getId());
        }

        final PacketTypeCommon packetType = event.getPacketType();
        if (packetType == PacketType.Play.Server.OPEN_WINDOW || packetType == PacketType.Play.Server.OPEN_HORSE_WINDOW) {
            player.sendTransaction();
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.serverOpenedInventoryThisTick = true);
        }

        if (packetType == PacketType.Play.Server.BUNDLE) {
            player.packetStateData.sendingBundlePacket = !player.packetStateData.sendingBundlePacket;
        }

        player.checkManager.onPreViaPacketSend(event);
    }
}
