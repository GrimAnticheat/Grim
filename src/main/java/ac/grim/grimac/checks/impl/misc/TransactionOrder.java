package ac.grim.grimac.checks.impl.misc;

import ac.grim.grimac.checks.AbstractPacketCheck;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "TransactionOrder")
public class TransactionOrder extends AbstractPacketCheck {
    public TransactionOrder(GrimPlayer player) {
        super(player);
    }
}
