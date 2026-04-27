package ac.grim.grimac.checks.impl.misc;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "TransactionOrder", stableKey = "grim.ping.invalid_transaction_order")
public class TransactionOrder extends Check {
    public TransactionOrder(GrimPlayer player) {
        super(player);
    }
}
