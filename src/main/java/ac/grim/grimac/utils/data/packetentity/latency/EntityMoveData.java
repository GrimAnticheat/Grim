package ac.grim.grimac.utils.data.packetentity.latency;

public class EntityMoveData {
    public final int entityID;
    public final double deltaX;
    public final double deltaY;
    public final double deltaZ;
    public final int lastTransactionSent;

    public EntityMoveData(int entityID, double deltaX, double deltaY, double deltaZ, int lastTransactionSent) {
        this.entityID = entityID;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.deltaZ = deltaZ;
        this.lastTransactionSent = lastTransactionSent;
    }
}
