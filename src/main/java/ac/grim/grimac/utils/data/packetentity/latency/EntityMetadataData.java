package ac.grim.grimac.utils.data.packetentity.latency;

public class EntityMetadataData {
    public final int entityID;
    public final Runnable runnable;
    public int lastTransactionSent;

    public EntityMetadataData(int entityID, Runnable runnable, int lastTransactionSent) {
        this.entityID = entityID;
        this.runnable = runnable;
        this.lastTransactionSent = lastTransactionSent;
    }
}
