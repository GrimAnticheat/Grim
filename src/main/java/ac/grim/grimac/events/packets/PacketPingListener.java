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
import net.kyori.adventure.text.Component;

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

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;
            // The client always replies with an accepted transaction
            // This check prevents some clients (and some bots) sending invalid packets
            if (!transaction.isAccepted()) {
                // Only kick the player if it's enabled in the configuration
                if (GrimAPI.INSTANCE.getConfigManager().isKickInvalidTransactions()) {
                    // Disconnect the player for sending an invalid transaction packet
                    player.disconnect(Component.text(String.format("Invalid transaction response (%d not accepted)", id)));
                }
                // Drop the packet to disallow further processing
                event.setCancelled(true);
                return;
            }
            player.packetStateData.lastTransactionPacketWasValid = false;

            // Vanilla always uses an ID starting from 1
            if (id <= 0) {
                // Check if we sent this packet before cancelling it
                if (player.addTransactionResponse(id)) {
                    player.packetStateData.lastTransactionPacketWasValid = true;
                    event.setCancelled(true);
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.PONG) {
            WrapperPlayClientPong pong = new WrapperPlayClientPong(event);
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;
            player.packetStateData.lastTransactionPacketWasValid = false;

            int id = pong.getId();
            // If it wasn't below 0, it wasn't us
            // If it wasn't in short range, it wasn't us either
            if (id == (short) id) {
                short shortID = ((short) id);
                if (player.addTransactionResponse(shortID)) {
                    player.packetStateData.lastTransactionPacketWasValid = true;
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
