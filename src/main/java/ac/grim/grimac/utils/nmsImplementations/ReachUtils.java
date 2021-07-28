package ac.grim.grimac.utils.nmsImplementations;


import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import org.bukkit.util.Vector;

public class ReachUtils {
    // Copied from 1.8 nms, don't ask me what it does.
    public static Vector calculateIntercept(SimpleCollisionBox self, Vector vecA, Vector vecB) {
        Vector vec3 = getIntermediateWithXValue(vecA, vecB, self.minX);
        Vector vec31 = getIntermediateWithXValue(vecA, vecB, self.maxX);
        Vector vec32 = getIntermediateWithYValue(vecA, vecB, self.minY);
        Vector vec33 = getIntermediateWithYValue(vecA, vecB, self.maxY);
        Vector vec34 = getIntermediateWithZValue(vecA, vecB, self.minZ);
        Vector vec35 = getIntermediateWithZValue(vecA, vecB, self.maxZ);

        if (!isVecInYZ(self, vec3)) {
            vec3 = null;
        }

        if (!isVecInYZ(self, vec31)) {
            vec31 = null;
        }

        if (!isVecInXZ(self, vec32)) {
            vec32 = null;
        }

        if (!isVecInXZ(self, vec33)) {
            vec33 = null;
        }

        if (!isVecInXY(self, vec34)) {
            vec34 = null;
        }

        if (!isVecInXY(self, vec35)) {
            vec35 = null;
        }

        Vector vec36 = null;

        if (vec3 != null) {
            vec36 = vec3;
        }

        if (vec31 != null && (vec36 == null || vecA.distanceSquared(vec31) < vecA.distanceSquared(vec36))) {
            vec36 = vec31;
        }

        if (vec32 != null && (vec36 == null || vecA.distanceSquared(vec32) < vecA.distanceSquared(vec36))) {
            vec36 = vec32;
        }

        if (vec33 != null && (vec36 == null || vecA.distanceSquared(vec33) < vecA.distanceSquared(vec36))) {
            vec36 = vec33;
        }

        if (vec34 != null && (vec36 == null || vecA.distanceSquared(vec34) < vecA.distanceSquared(vec36))) {
            vec36 = vec34;
        }

        if (vec35 != null && (vec36 == null || vecA.distanceSquared(vec35) < vecA.distanceSquared(vec36))) {
            vec36 = vec35;
        }

        return vec36;
    }

    /**
     * Returns a new vector with x value equal to the second parameter, along the line between this vector and the
     * passed in vector, or null if not possible.
     */
    public static Vector getIntermediateWithXValue(Vector self, Vector other, double x) {
        double d0 = other.getX() - self.getX();
        double d1 = other.getY() - self.getY();
        double d2 = other.getZ() - self.getZ();

        if (d0 * d0 < 1.0000000116860974E-7D) {
            return null;
        } else {
            double d3 = (x - self.getX()) / d0;
            return d3 >= 0.0D && d3 <= 1.0D ? new Vector(self.getX() + d0 * d3, self.getY() + d1 * d3, self.getZ() + d2 * d3) : null;
        }
    }

    /**
     * Returns a new vector with y value equal to the second parameter, along the line between this vector and the
     * passed in vector, or null if not possible.
     */
    public static Vector getIntermediateWithYValue(Vector self, Vector other, double y) {
        double d0 = other.getX() - self.getX();
        double d1 = other.getY() - self.getY();
        double d2 = other.getZ() - self.getZ();

        if (d1 * d1 < 1.0000000116860974E-7D) {
            return null;
        } else {
            double d3 = (y - self.getY()) / d1;
            return d3 >= 0.0D && d3 <= 1.0D ? new Vector(self.getX() + d0 * d3, self.getY() + d1 * d3, self.getZ() + d2 * d3) : null;
        }
    }

    /**
     * Returns a new vector with z value equal to the second parameter, along the line between this vector and the
     * passed in vector, or null if not possible.
     */
    public static Vector getIntermediateWithZValue(Vector self, Vector other, double z) {
        double d0 = other.getX() - self.getX();
        double d1 = other.getY() - self.getY();
        double d2 = other.getZ() - self.getZ();

        if (d2 * d2 < 1.0000000116860974E-7D) {
            return null;
        } else {
            double d3 = (z - self.getZ()) / d2;
            return d3 >= 0.0D && d3 <= 1.0D ? new Vector(self.getX() + d0 * d3, self.getY() + d1 * d3, self.getZ() + d2 * d3) : null;
        }
    }

    /**
     * Checks if the specified vector is within the YZ dimensions of the bounding box. Args: Vec3D
     */
    private static boolean isVecInYZ(SimpleCollisionBox self, Vector vec) {
        return vec != null && vec.getY() >= self.minY && vec.getY() <= self.maxY && vec.getZ() >= self.minZ && vec.getZ() <= self.maxZ;
    }

    /**
     * Checks if the specified vector is within the XZ dimensions of the bounding box. Args: Vec3D
     */
    private static boolean isVecInXZ(SimpleCollisionBox self, Vector vec) {
        return vec != null && vec.getX() >= self.minX && vec.getX() <= self.maxX && vec.getZ() >= self.minZ && vec.getZ() <= self.maxZ;
    }

    /**
     * Checks if the specified vector is within the XY dimensions of the bounding box. Args: Vec3D
     */
    private static boolean isVecInXY(SimpleCollisionBox self, Vector vec) {
        return vec != null && vec.getX() >= self.minX && vec.getX() <= self.maxX && vec.getY() >= self.minY && vec.getY() <= self.maxY;
    }

    // Look vector accounting for optifine shitmath
    public static Vector getLook(GrimPlayer player, float xRot, float yRot) {
        float f = player.trigHandler.cos(-xRot * 0.017453292F - (float) Math.PI);
        float f1 = player.trigHandler.sin(-xRot * 0.017453292F - (float) Math.PI);
        float f2 = -player.trigHandler.cos(-yRot * 0.017453292F);
        float f3 = player.trigHandler.sin(-yRot * 0.017453292F);
        return new Vector(f1 * f2, f3, (double) (f * f2));
    }

    public static boolean isVecInside(SimpleCollisionBox self, Vector vec) {
        return vec.getX() > self.minX && vec.getX() < self.maxX && (vec.getY() > self.minY && vec.getY() < self.maxY && vec.getZ() > self.minZ && vec.getZ() < self.maxZ);
    }
}
