package ac.grim.grimac.checks.impl.misc;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientWindowConfirmation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowConfirmation;

import java.util.ArrayList;

@CheckData(name = "TransactionOrder", experimental = true)
public class TransactionOrder extends Check implements PacketCheck {
    private final ArrayList<Integer> transactionOrder = new ArrayList<>();
    private boolean atomicBoolean = false;

    public TransactionOrder(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.WINDOW_CONFIRMATION) {
            WrapperPlayClientWindowConfirmation transaction = new WrapperPlayClientWindowConfirmation(event);

            if (transaction.getWindowId() == 0) {
                onTransactionReceive(transaction.getActionId());
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.PONG) {
            onTransactionReceive(new WrapperPlayClientPong(event).getId());
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.WINDOW_CONFIRMATION) {
            WrapperPlayServerWindowConfirmation transaction = new WrapperPlayServerWindowConfirmation(event);

            if (transaction.getWindowId() == 0 && !transaction.isAccepted()) {
                transactionOrder.add((int) transaction.getActionId());
            }
        }

        if (event.getPacketType() == PacketType.Play.Server.PING) {
            transactionOrder.add(new WrapperPlayServerPing(event).getId());
        }
    }

    private void onTransactionReceive(int id) {
        if (transactionOrder.isEmpty()) {
            flagAndAlert(String.format("Expected: %s | Received: %d", "None", id));
            return;
        }

        int expected = transactionOrder.get(0);

        if (expected != id) {
            flagAndAlert(String.format("Expected: %d | Received: %d", expected, id));
        }

        if (!transactionOrder.contains(id)) return;

        transactionOrder.removeIf(transaction -> {
            if (atomicBoolean)
                return false;

            if (transaction == id)
                atomicBoolean = true;
            return true;
        });
        atomicBoolean = false;
    }
}