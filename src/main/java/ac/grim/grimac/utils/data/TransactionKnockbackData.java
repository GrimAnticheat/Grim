package ac.grim.grimac.utils.data;

import org.bukkit.util.Vector;

public class TransactionKnockbackData {
    public final int transactionID;
    public final int entityID;
    public final Vector knockback;

    public TransactionKnockbackData(int transactionID, int entityID, Vector knockback) {
        this.transactionID = transactionID;
        this.entityID = entityID;
        this.knockback = knockback;
    }
}
