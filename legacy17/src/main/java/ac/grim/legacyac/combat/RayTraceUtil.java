package ac.grim.legacyac.combat;

import org.bukkit.util.Vector;

/**
 * Ray-tracing utilities for combat checks.
 * Implements the same ray-AABB intersection algorithm used in vanilla Minecraft
 * (and mirrored by GrimAC's ReachUtils.calculateIntercept).
 */
public final class RayTraceUtil {
    private RayTraceUtil() {
    }

    /**
     * Check if a ray intersects an AABB within a given max distance (slab method).
     */
    public static boolean intersectsAabb(Vector origin, Vector direction, double maxDistance, HitboxFrame box) {
        double tMin = 0.0D;
        double tMax = maxDistance;

        double[] o = new double[] { origin.getX(), origin.getY(), origin.getZ() };
        double[] d = new double[] { direction.getX(), direction.getY(), direction.getZ() };
        double[] min = new double[] { box.getMinX(), box.getMinY(), box.getMinZ() };
        double[] max = new double[] { box.getMaxX(), box.getMaxY(), box.getMaxZ() };

        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < 1.0E-7D) {
                if (o[i] < min[i] || o[i] > max[i]) {
                    return false;
                }
            } else {
                double inv = 1.0D / d[i];
                double t1 = (min[i] - o[i]) * inv;
                double t2 = (max[i] - o[i]) * inv;
                if (t1 > t2) {
                    double tmp = t1;
                    t1 = t2;
                    t2 = tmp;
                }
                tMin = Math.max(tMin, t1);
                tMax = Math.min(tMax, t2);
                if (tMax < tMin) {
                    return false;
                }
            }
        }

        return tMax >= 0.0D && tMin <= maxDistance;
    }

    /**
     * Calculate the closest intersection point between a ray and an AABB.
     * This mirrors vanilla MC's calculation and Grim's calculateIntercept approach.
     *
     * @param origin    the ray origin (eye position)
     * @param direction the normalized direction vector
     * @param maxRange  maximum ray length
     * @param box       the target AABB
     * @return the distance to the closest intersection point, or Double.MAX_VALUE
     *         if no intersection
     */
    public static double intersectionDistance(Vector origin, Vector direction, double maxRange, HitboxFrame box) {
        // Use the vanilla approach: check intersection with each face of the AABB,
        // verify the intersection point is within the other two axes' bounds,
        // and return the closest valid intersection.

        Vector end = origin.clone().add(direction.clone().multiply(maxRange));

        Vector best = null;

        // Check each face pair (X, Y, Z min and max)
        Vector minX = getIntermediateWithXValue(origin, end, box.getMinX());
        Vector maxX = getIntermediateWithXValue(origin, end, box.getMaxX());
        Vector minY = getIntermediateWithYValue(origin, end, box.getMinY());
        Vector maxY = getIntermediateWithYValue(origin, end, box.getMaxY());
        Vector minZ = getIntermediateWithZValue(origin, end, box.getMinZ());
        Vector maxZ = getIntermediateWithZValue(origin, end, box.getMaxZ());

        if (!isVecInYZ(box, minX))
            minX = null;
        if (!isVecInYZ(box, maxX))
            maxX = null;
        if (!isVecInXZ(box, minY))
            minY = null;
        if (!isVecInXZ(box, maxY))
            maxY = null;
        if (!isVecInXY(box, minZ))
            minZ = null;
        if (!isVecInXY(box, maxZ))
            maxZ = null;

        if (minX != null)
            best = minX;
        if (maxX != null && (best == null || origin.distanceSquared(maxX) < origin.distanceSquared(best)))
            best = maxX;
        if (minY != null && (best == null || origin.distanceSquared(minY) < origin.distanceSquared(best)))
            best = minY;
        if (maxY != null && (best == null || origin.distanceSquared(maxY) < origin.distanceSquared(best)))
            best = maxY;
        if (minZ != null && (best == null || origin.distanceSquared(minZ) < origin.distanceSquared(best)))
            best = minZ;
        if (maxZ != null && (best == null || origin.distanceSquared(maxZ) < origin.distanceSquared(best)))
            best = maxZ;

        if (best == null) {
            return Double.MAX_VALUE;
        }
        return origin.distance(best);
    }

    /**
     * Check if the origin point is inside the AABB.
     */
    public static boolean isVecInside(Vector vec, HitboxFrame box) {
        return vec.getX() > box.getMinX() && vec.getX() < box.getMaxX()
                && vec.getY() > box.getMinY() && vec.getY() < box.getMaxY()
                && vec.getZ() > box.getMinZ() && vec.getZ() < box.getMaxZ();
    }

    // --- Vanilla-style intermediate point calculations ---

    private static Vector getIntermediateWithXValue(Vector origin, Vector end, double x) {
        double dx = end.getX() - origin.getX();
        double dy = end.getY() - origin.getY();
        double dz = end.getZ() - origin.getZ();
        if (dx * dx < 1.0E-7D)
            return null;
        double t = (x - origin.getX()) / dx;
        if (t < 0.0D || t > 1.0D)
            return null;
        return new Vector(origin.getX() + dx * t, origin.getY() + dy * t, origin.getZ() + dz * t);
    }

    private static Vector getIntermediateWithYValue(Vector origin, Vector end, double y) {
        double dx = end.getX() - origin.getX();
        double dy = end.getY() - origin.getY();
        double dz = end.getZ() - origin.getZ();
        if (dy * dy < 1.0E-7D)
            return null;
        double t = (y - origin.getY()) / dy;
        if (t < 0.0D || t > 1.0D)
            return null;
        return new Vector(origin.getX() + dx * t, origin.getY() + dy * t, origin.getZ() + dz * t);
    }

    private static Vector getIntermediateWithZValue(Vector origin, Vector end, double z) {
        double dx = end.getX() - origin.getX();
        double dy = end.getY() - origin.getY();
        double dz = end.getZ() - origin.getZ();
        if (dz * dz < 1.0E-7D)
            return null;
        double t = (z - origin.getZ()) / dz;
        if (t < 0.0D || t > 1.0D)
            return null;
        return new Vector(origin.getX() + dx * t, origin.getY() + dy * t, origin.getZ() + dz * t);
    }

    private static boolean isVecInYZ(HitboxFrame box, Vector vec) {
        return vec != null && vec.getY() >= box.getMinY() && vec.getY() <= box.getMaxY()
                && vec.getZ() >= box.getMinZ() && vec.getZ() <= box.getMaxZ();
    }

    private static boolean isVecInXZ(HitboxFrame box, Vector vec) {
        return vec != null && vec.getX() >= box.getMinX() && vec.getX() <= box.getMaxX()
                && vec.getZ() >= box.getMinZ() && vec.getZ() <= box.getMaxZ();
    }

    private static boolean isVecInXY(HitboxFrame box, Vector vec) {
        return vec != null && vec.getX() >= box.getMinX() && vec.getX() <= box.getMaxX()
                && vec.getY() >= box.getMinY() && vec.getY() <= box.getMaxY();
    }
}
