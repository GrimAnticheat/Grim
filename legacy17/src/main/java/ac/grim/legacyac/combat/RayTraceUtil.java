package ac.grim.legacyac.combat;

import org.bukkit.util.Vector;

public final class RayTraceUtil {
    private RayTraceUtil() {
    }

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
}
