package ac.grim.grimac.utils.data;

import org.bukkit.util.Vector;

public class VelocityData {
    public final Vector vector;
    public double offset = Integer.MAX_VALUE;
    public final int entityID;

    public VelocityData(int entityID, Vector vector) {
        this.entityID = entityID;
        this.vector = vector;
    }
}
