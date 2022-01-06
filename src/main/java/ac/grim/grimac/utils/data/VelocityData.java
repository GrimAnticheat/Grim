package ac.grim.grimac.utils.data;

import org.bukkit.util.Vector;

public class VelocityData {
    public final Vector vector;
    public final int entityID;
    public final int transaction;
    public double offset = Integer.MAX_VALUE;

    public VelocityData(int entityID, int transaction, Vector vector) {
        this.entityID = entityID;
        this.vector = vector;
        this.transaction = transaction;
    }

    // First bread last tick -> Required this tick = don't require kb twice
    public VelocityData(int entityID, int transaction, Vector vector, double offset) {
        this.entityID = entityID;
        this.vector = vector;
        this.transaction = transaction;
        this.offset = offset;
    }
}
