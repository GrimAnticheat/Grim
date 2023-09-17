package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.impl.misc.TransactionOrder;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.Pair;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientWindowConfirmation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowConfirmation;

public class PacketPingListener extends PacketListenerAbstract {

    // Must listen on LOWEST (or maybe low) to stop Tuinity packet limiter from kicking players for transaction/pong spam
    public PacketPingListener() {
        super(PacketListenerPriority.LOWEST);
    }


    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.WINDOW_CONFIRMATION) {
            WrapperPlayClientWindowConfirmation transaction = new WrapperPlayClientWindowConfirmation(event);
            short id = transaction.getActionId();

            // Vanilla always uses an ID starting from 1
            if (id <= 0) {
                GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
                if (player == null) return;

                // Check if we sent this packet before cancelling it
                if (player.addTransactionResponse(id)) {
                    event.setCancelled(true);
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.PONG) {
            WrapperPlayClientPong pong = new WrapperPlayClientPong(event);

            int id = pong.getId();
            // If it wasn't below 0, it wasn't us
            // If it wasn't in short range, it wasn't us either
            if (id == (short) id) {
                GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
                if (player == null) return;
                short shortID = ((short) id);
                if (player.addTransactionResponse(shortID)) {
                    // Not needed for vanilla as vanilla ignores this packet, needed for packet limiters
                    event.setCancelled(true);
                }
            }
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.WINDOW_CONFIRMATION) {
            WrapperPlayServerWindowConfirmation confirmation = new WrapperPlayServerWindowConfirmation(event);
            short id = confirmation.getActionId();

            // Vanilla always uses an ID starting from 1
            if (id <= 0) {
                GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
                if (player == null) return;

                if (player.didWeSendThatTrans.remove((Short) id)) {
                    player.transactionsSent.add(new Pair<>(id, System.nanoTime()));
                    player.lastTransactionSent.getAndIncrement();
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Server.PING) {
            WrapperPlayServerPing pong = new WrapperPlayServerPing(event);

            int id = pong.getId();
            // Check if in the short range, we only use short range
            if (id == (short) id) {
                GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
                if (player == null) return;
                // Cast ID twice so we can use the list
                Short shortID = ((short) id);
                if (player.didWeSendThatTrans.remove(shortID)) {
                    player.transactionsSent.add(new Pair<>(shortID, System.nanoTime()));
                    player.lastTransactionSent.getAndIncrement();
                }
            }
        }
    }


}
