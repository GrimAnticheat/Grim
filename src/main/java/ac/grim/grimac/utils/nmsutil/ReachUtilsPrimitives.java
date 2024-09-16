package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.Pair;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;

public class ReachUtilsPrimitives {
    public static Pair<double[], BlockFace> calculateIntercept(SimpleCollisionBox self, double[] origin, double[] end) {
        double[] minX = getIntermediateWithXValue(origin, end, self.minX);
        double[] maxX = getIntermediateWithXValue(origin, end, self.maxX);
        double[] minY = getIntermediateWithYValue(origin, end, self.minY);
        double[] maxY = getIntermediateWithYValue(origin, end, self.maxY);
        double[] minZ = getIntermediateWithZValue(origin, end, self.minZ);
        double[] maxZ = getIntermediateWithZValue(origin, end, self.maxZ);

        BlockFace bestFace = null;

        if (!isVecInYZ(self, minX)) minX = null;
        if (!isVecInYZ(self, maxX)) maxX = null;
        if (!isVecInXZ(self, minY)) minY = null;
        if (!isVecInXZ(self, maxY)) maxY = null;
        if (!isVecInXY(self, minZ)) minZ = null;
        if (!isVecInXY(self, maxZ)) maxZ = null;

        double[] best = null;
        double bestDistSq = Double.MAX_VALUE;

        if (minX != null) {
            best = minX;
            bestDistSq = distanceSquared(origin, minX);
            bestFace = BlockFace.WEST;
        }

        if (maxX != null) {
            double distSq = distanceSquared(origin, maxX);
            if (best == null || distSq < bestDistSq) {
                best = maxX;
                bestDistSq = distSq;
                bestFace = BlockFace.EAST;
            }
        }

        if (minY != null) {
            double distSq = distanceSquared(origin, minY);
            if (best == null || distSq < bestDistSq) {
                best = minY;
                bestDistSq = distSq;
                bestFace = BlockFace.DOWN;
            }
        }

        if (maxY != null) {
            double distSq = distanceSquared(origin, maxY);
            if (best == null || distSq < bestDistSq) {
                best = maxY;
                bestDistSq = distSq;
                bestFace = BlockFace.UP;
            }
        }

        if (minZ != null) {
            double distSq = distanceSquared(origin, minZ);
            if (best == null || distSq < bestDistSq) {
                best = minZ;
                bestDistSq = distSq;
                bestFace = BlockFace.NORTH;
            }
        }

        if (maxZ != null) {
            double distSq = distanceSquared(origin, maxZ);
            if (best == null || distSq < bestDistSq) {
                best = maxZ;
                bestFace = BlockFace.SOUTH;
            }
        }

        return new Pair<>(best, bestFace);
    }

    private static double[] getIntermediateWithXValue(double[] self, double[] other, double x) {
        double d0 = other[0] - self[0];
        double d1 = other[1] - self[1];
        double d2 = other[2] - self[2];

        if (d0 * d0 < 1.0000000116860974E-7D) {
            return null;
        } else {
            double d3 = (x - self[0]) / d0;
            return d3 >= 0.0D && d3 <= 1.0D ? new double[]{self[0] + d0 * d3, self[1] + d1 * d3, self[2] + d2 * d3} : null;
        }
    }

    private static double[] getIntermediateWithYValue(double[] self, double[] other, double y) {
        double d0 = other[0] - self[0];
        double d1 = other[1] - self[1];
        double d2 = other[2] - self[2];

        if (d1 * d1 < 1.0000000116860974E-7D) {
            return null;
        } else {
            double d3 = (y - self[1]) / d1;
            return d3 >= 0.0D && d3 <= 1.0D ? new double[]{self[0] + d0 * d3, self[1] + d1 * d3, self[2] + d2 * d3} : null;
        }
    }

    private static double[] getIntermediateWithZValue(double[] self, double[] other, double z) {
        double d0 = other[0] - self[0];
        double d1 = other[1] - self[1];
        double d2 = other[2] - self[2];

        if (d2 * d2 < 1.0000000116860974E-7D) {
            return null;
        } else {
            double d3 = (z - self[2]) / d2;
            return d3 >= 0.0D && d3 <= 1.0D ? new double[]{self[0] + d0 * d3, self[1] + d1 * d3, self[2] + d2 * d3} : null;
        }
    }

    private static boolean isVecInYZ(SimpleCollisionBox self, double[] vec) {
        return vec != null && vec[1] >= self.minY && vec[1] <= self.maxY && vec[2] >= self.minZ && vec[2] <= self.maxZ;
    }

    private static boolean isVecInXZ(SimpleCollisionBox self, double[] vec) {
        return vec != null && vec[0] >= self.minX && vec[0] <= self.maxX && vec[2] >= self.minZ && vec[2] <= self.maxZ;
    }

    private static boolean isVecInXY(SimpleCollisionBox self, double[] vec) {
        return vec != null && vec[0] >= self.minX && vec[0] <= self.maxX && vec[1] >= self.minY && vec[1] <= self.maxY;
    }

    public static boolean isVecInside(SimpleCollisionBox self, double[] vec) {
        return vec[0] > self.minX && vec[0] < self.maxX &&
                vec[1] > self.minY && vec[1] < self.maxY &&
                vec[2] > self.minZ && vec[2] < self.maxZ;
    }

    private static double distanceSquared(double[] vec1, double[] vec2) {
        double dx = vec1[0] - vec2[0];
        double dy = vec1[1] - vec2[1];
        double dz = vec1[2] - vec2[2];
        return dx * dx + dy * dy + dz * dz;
    }

    // The following methods remain unchanged but are included for completeness

    public static double[] getLook(GrimPlayer player, float yaw, float pitch) {
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_12_2)) {
            float f = player.trigHandler.cos(-yaw * 0.017453292F - (float)Math.PI);
            float f1 = player.trigHandler.sin(-yaw * 0.017453292F - (float)Math.PI);
            float f2 = -player.trigHandler.cos(-pitch * 0.017453292F);
            float f3 = player.trigHandler.sin(-pitch * 0.017453292F);
            return new double[]{f1 * f2, f3, f * f2};
        } else {
            float f = pitch * ((float) Math.PI / 180F);
            float f1 = -yaw * ((float) Math.PI / 180F);
            float f2 = player.trigHandler.cos(f1);
            float f3 = player.trigHandler.sin(f1);
            float f4 = player.trigHandler.cos(f);
            float f5 = player.trigHandler.sin(f);
            return new double[]{f3 * f4, -f5, f2 * f4};
        }
    }

//    public static double getMinReachToBox(GrimPlayer player, SimpleCollisionBox targetBox) {
//        boolean giveMovementThresholdLenience = player.packetStateData.didLastMovementIncludePosition || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9);
//        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) targetBox.expand(0.1);
//
//        double lowest = Double.MAX_VALUE;
//
//        for (double eyes : player.getPossibleEyeHeights()) {
//            if (giveMovementThresholdLenience) targetBox.expand(player.getMovementThreshold());
//            double[] from = new double[]{player.x, player.y + eyes, player.z};
//            double[] closestPoint = VectorUtils.cutBoxToVector(from, targetBox);
//            lowest = Math.min(lowest, Math.sqrt(distanceSquared(from, closestPoint)));
//        }
//
//        return lowest;
//    }
}
