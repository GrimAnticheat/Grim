package ac.grim.grimac.checks.impl.misc;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.CheckType;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;

import java.util.ArrayList;

@CheckData(name = "TransactionOrder", checkType = CheckType.PACKETS)
public class TransactionOrder extends Check implements PacketCheck {

    public TransactionOrder(GrimPlayer player) {
        super(player);
    }

}