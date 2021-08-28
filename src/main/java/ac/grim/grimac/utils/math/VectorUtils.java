package ac.grim.grimac.utils.math;

import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.util.Vector;

public class VectorUtils {
    public static Vector cutBoxToVector(Vector vectorCutTo, SimpleCollisionBox box) {
        return cutBoxToVector(vectorCutTo, box.min(), box.max());
    }

    public static Vector cutBoxToVector(Vector vectorToCutTo, Vector min, Vector max) {
        double xMin = Math.min(min.getX(), max.getX());
        double xMax = Math.max(min.getX(), max.getX());
        double yMin = Math.min(min.getY(), max.getY());
        double yMax = Math.max(min.getY(), max.getY());
        double zMin = Math.min(min.getZ(), max.getZ());
        double zMax = Math.max(min.getZ(), max.getZ());

        Vector cutCloned = vectorToCutTo.clone();

        if (xMin > vectorToCutTo.getX() || xMax < vectorToCutTo.getX()) {
            if (Math.abs(vectorToCutTo.getX() - xMin) < Math.abs(vectorToCutTo.getX() - xMax)) {
                cutCloned.setX(xMin);
            } else {
                cutCloned.setX(xMax);
            }
        }

        if (yMin > vectorToCutTo.getY() || yMax < vectorToCutTo.getY()) {
            if (Math.abs(vectorToCutTo.getY() - yMin) < Math.abs(vectorToCutTo.getY() - yMax)) {
                cutCloned.setY(yMin);
            } else {
                cutCloned.setY(yMax);
            }
        }

        if (zMin > vectorToCutTo.getZ() || zMax < vectorToCutTo.getZ()) {
            if (Math.abs(vectorToCutTo.getZ() - zMin) < Math.abs(vectorToCutTo.getZ() - zMax)) {
                cutCloned.setZ(zMin);
            } else {
                cutCloned.setZ(zMax);
            }
        }

        return cutCloned;
    }

    public static Vector fromVec3d(Vector3d vector3d) {
        return new Vector(vector3d.getX(), vector3d.getY(), vector3d.getZ());
    }
}
