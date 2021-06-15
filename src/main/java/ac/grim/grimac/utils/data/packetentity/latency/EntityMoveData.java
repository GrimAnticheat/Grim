package ac.grim.grimac.utils.data.packetentity.latency;

public class EntityMoveData {
    public final int entityID;
    public final double x;
    public final double y;
    public final double z;
    public final int lastTransactionSent;
    public final boolean isRelative;

    public EntityMoveData(int entityID, double x, double y, double z, int lastTransactionSent, boolean isRelative) {
        this.entityID = entityID;
        this.x = x;
        this.y = y;
        this.z = z;
        this.lastTransactionSent = lastTransactionSent;
        this.isRelative = isRelative;
    }
}
