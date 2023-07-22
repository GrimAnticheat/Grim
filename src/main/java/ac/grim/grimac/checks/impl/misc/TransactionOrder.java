package ac.grim.grimac.checks.impl.misc;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "TransactionOrder", experimental = true)
public class TransactionOrder extends Check implements PacketCheck {
    private boolean atomicBoolean = false;

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

        int expected = player.transactionOrder.get(0);

        if (expected != id) {
            flagAndAlert(String.format("Expected: %d | Received: %d", expected, id));
        }

        if (!player.transactionOrder.contains(id)) return;

        player.transactionOrder.removeIf(transaction -> {
            if (atomicBoolean)
                return false;

            if (transaction == id)
                atomicBoolean = true;
            return true;
        });
        atomicBoolean = false;
    }
}