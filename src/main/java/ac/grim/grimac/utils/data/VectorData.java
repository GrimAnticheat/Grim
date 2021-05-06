package ac.grim.grimac.utils.data;

import org.bukkit.util.Vector;

public class VectorData {
    public VectorType vectorType;
    public Vector vector;

    public VectorData(Vector vector, VectorType vectorType) {
        this.vector = vector;
        this.vectorType = vectorType;
    }

    public VectorData(double x, double y, double z, VectorType vectorType) {
        this.vector = new Vector(x, y, z);
        this.vectorType = vectorType;
    }

    public enum VectorType {
        Normal,
        Swimhop,
        Ladder,
        Knockback,
        Hackyladder
    }
}
