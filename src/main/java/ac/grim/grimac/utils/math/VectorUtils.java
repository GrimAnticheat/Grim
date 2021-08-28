package ac.grim.grimac.utils.math;

import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.util.Vector;

public class VectorUtils {
    public static Vector cutBoxToVector(Vector vectorToCutTo, Vector min, Vector max) {
        SimpleCollisionBox box = new SimpleCollisionBox(min, max).sort();
        return cutBoxToVector(vectorToCutTo, box);
    }

    public static Vector cutBoxToVector(Vector vectorCutTo, SimpleCollisionBox box) {
        return new Vector(GrimMath.clamp(vectorCutTo.getX(), box.minX, box.maxX),
                GrimMath.clamp(vectorCutTo.getY(), box.minY, box.maxY),
                GrimMath.clamp(vectorCutTo.getZ(), box.minZ, box.maxZ));
    }

    public static Vector fromVec3d(Vector3d vector3d) {
        return new Vector(vector3d.getX(), vector3d.getY(), vector3d.getZ());
    }
}
