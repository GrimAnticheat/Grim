package ac.grim.grimac.utils.math;

import org.bukkit.util.Vector;

public class VectorUtils {
    public static Vector cutVectorsToPlayerMovement(Vector vectorToCutTo, Vector vectorOne, Vector vectorTwo) {
        double xMin = Math.min(vectorOne.getX(), vectorTwo.getX());
        double xMax = Math.max(vectorOne.getX(), vectorTwo.getX());
        double yMin = Math.min(vectorOne.getY(), vectorTwo.getY());
        double yMax = Math.max(vectorOne.getY(), vectorTwo.getY());
        double zMin = Math.min(vectorOne.getZ(), vectorTwo.getZ());
        double zMax = Math.max(vectorOne.getZ(), vectorTwo.getZ());

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
}
