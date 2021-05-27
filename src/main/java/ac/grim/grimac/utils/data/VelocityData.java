package ac.grim.grimac.utils.data;

import org.bukkit.util.Vector;

public class VelocityData {
    public final Vector vector;
    public double offset = Integer.MAX_VALUE;

    public VelocityData(Vector vector) {
        this.vector = vector;
    }
}
