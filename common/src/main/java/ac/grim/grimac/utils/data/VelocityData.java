package ac.grim.grimac.utils.data;

import ac.grim.grimac.utils.math.Vector3dm;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class VelocityData {
    public final int entityID;
    public final int transaction;
    public final boolean isSetback;
    public final @NotNull Vector3dm vector;
    public double offset = Integer.MAX_VALUE;

    public VelocityData(int entityID, int transaction, boolean isSetback, @NotNull Vector3dm vector) {
        this.entityID = entityID;
        this.transaction = transaction;
        this.isSetback = isSetback;
        this.vector = Objects.requireNonNull(vector, "vector");
    }
}
