package ac.grim.grimac.checks.impl.misc;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;

import java.util.ArrayList;

@CheckData(name = "TransactionOrder", experimental = true)
public class TransactionOrder extends Check implements PacketCheck {

    public TransactionOrder(GrimPlayer player) {
        super(player);
    }

    public void onTransactionReceive(short id) {
        if (player.joinTime < 5000) {
            return;
        }

        if (player.transactionOrder.isEmpty()) {
            flagAndAlert(String.format("Expected: %s | Received: %d", "None", id));
            return;
        }

        ArrayList<Short> transactions = new ArrayList<>(player.transactionOrder);

        int expected = transactions.get(0);

        if (expected != id) {
            flagAndAlert(String.format("Expected: %d | Received: %d", expected, id));
        }

        if (transactions.contains(id)) {
            int index = transactions.indexOf(id);
            transactions.subList(0, index + 1).clear();
            player.transactionOrder.clear();
            player.transactionOrder.addAll(transactions);
        }

    }
}