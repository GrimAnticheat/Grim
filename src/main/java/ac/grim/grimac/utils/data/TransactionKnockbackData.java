package ac.grim.grimac.utils.data;

import org.bukkit.util.Vector;

public class TransactionKnockbackData {
    public final int transactionID;
    public final Integer entityID;
    public final Vector knockback;

    public TransactionKnockbackData(int transactionID, Integer entityID, Vector knockback) {
        this.transactionID = transactionID;
        this.entityID = entityID;
        this.knockback = knockback;
    }
}
