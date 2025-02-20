package ac.grim.grimac.utils.nmsutil;


import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.VectorUtils;
import ac.grim.grimac.world.Vector3dm;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;

public class ReachUtils {
    // Copied from 1.8... I couldn't figure out 1.14+. "Enterprise" java code is unreadable!
    public static Pair<Vector3dm, BlockFace> calculateIntercept(SimpleCollisionBox self, Vector3dm origin, Vector3dm end) {
        Vector3dm minX = getIntermediateWithXValue(origin, end, self.minX);
        Vector3dm maxX = getIntermediateWithXValue(origin, end, self.maxX);
        Vector3dm minY = getIntermediateWithYValue(origin, end, self.minY);
        Vector3dm maxY = getIntermediateWithYValue(origin, end, self.maxY);
        Vector3dm minZ = getIntermediateWithZValue(origin, end, self.minZ);
        Vector3dm maxZ = getIntermediateWithZValue(origin, end, self.maxZ);

        BlockFace bestFace = null;

        if (!isVecInYZ(self, minX)) minX = null;
        if (!isVecInYZ(self, maxX)) maxX = null;
        if (!isVecInXZ(self, minY)) minY = null;
        if (!isVecInXZ(self, maxY)) maxY = null;
        if (!isVecInXY(self, minZ)) minZ = null;
        if (!isVecInXY(self, maxZ)) maxZ = null;

        Vector3dm best = null;

        if (minX != null) {
            best = minX;
            bestFace = BlockFace.WEST;
        }

        if (maxX != null && (best == null || origin.distanceSquared(maxX) < origin.distanceSquared(best))) {
            best = maxX;
            bestFace = BlockFace.EAST;
        }

        if (minY != null && (best == null || origin.distanceSquared(minY) < origin.distanceSquared(best))) {
            best = minY;
            bestFace = BlockFace.DOWN;
        }

        if (maxY != null && (best == null || origin.distanceSquared(maxY) < origin.distanceSquared(best))) {
            best = maxY;
            bestFace = BlockFace.UP;
        }

        if (minZ != null && (best == null || origin.distanceSquared(minZ) < origin.distanceSquared(best))) {
            best = minZ;
            bestFace = BlockFace.NORTH;
        }

        if (maxZ != null && (best == null || origin.distanceSquared(maxZ) < origin.distanceSquared(best))) {
            best = maxZ;
            bestFace = BlockFace.SOUTH;
        }

        return new Pair<>(best, bestFace);
    }

    /**
     * Returns a new vector with x value equal to the second parameter, along the line between this vector and the
     * passed in vector, or null if not possible.
     */
    public static Vector3dm getIntermediateWithXValue(Vector3dm self, Vector3dm other, double x) {
        double deltaX = other.getX() - self.getX();
        double deltaY = other.getY() - self.getY();
        double deltaZ = other.getZ() - self.getZ();

        if (deltaX * deltaX < 1.0000000116860974E-7D) {
            return null;
        } else {
            double d3 = (x - self.getX()) / deltaX;
            return d3 >= 0.0D && d3 <= 1.0D ? new Vector3dm(self.getX() + deltaX * d3, self.getY() + deltaY * d3, self.getZ() + deltaZ * d3) : null;
        }
    }

    /**
     * Returns a new vector with y value equal to the second parameter, along the line between this vector and the
     * passed in vector, or null if not possible.
     */
    public static Vector3dm getIntermediateWithYValue(Vector3dm self, Vector3dm other, double y) {
        double deltaX = other.getX() - self.getX();
        double deltaY = other.getY() - self.getY();
        double deltaZ = other.getZ() - self.getZ();

        if (deltaY * deltaY < 1.0000000116860974E-7D) {
            return null;
        } else {
            double d3 = (y - self.getY()) / deltaY;
            return d3 >= 0.0D && d3 <= 1.0D ? new Vector3dm(self.getX() + deltaX * d3, self.getY() + deltaY * d3, self.getZ() + deltaZ * d3) : null;
        }
    }

    /**
     * Returns a new vector with z value equal to the second parameter, along the line between this vector and the
     * passed in vector, or null if not possible.
     */
    public static Vector3dm getIntermediateWithZValue(Vector3dm self, Vector3dm other, double z) {
        double deltaX = other.getX() - self.getX();
        double deltaY = other.getY() - self.getY();
        double deltaZ = other.getZ() - self.getZ();

        if (deltaZ * deltaZ < 1.0000000116860974E-7D) {
            return null;
        } else {
            double d3 = (z - self.getZ()) / deltaZ;
            return d3 >= 0.0D && d3 <= 1.0D ? new Vector3dm(self.getX() + deltaX * d3, self.getY() + deltaY * d3, self.getZ() + deltaZ * d3) : null;
        }
    }

    /**
     * Checks if the specified vector is within the YZ dimensions of the bounding box. Args: Vec3D
     */
    private static boolean isVecInYZ(SimpleCollisionBox self, Vector3dm vec) {
        return vec != null && vec.getY() >= self.minY && vec.getY() <= self.maxY && vec.getZ() >= self.minZ && vec.getZ() <= self.maxZ;
    }

    /**
     * Checks if the specified vector is within the XZ dimensions of the bounding box. Args: Vec3D
     */
    private static boolean isVecInXZ(SimpleCollisionBox self, Vector3dm vec) {
        return vec != null && vec.getX() >= self.minX && vec.getX() <= self.maxX && vec.getZ() >= self.minZ && vec.getZ() <= self.maxZ;
    }

    /**
     * Checks if the specified vector is within the XY dimensions of the bounding box. Args: Vec3D
     */
    private static boolean isVecInXY(SimpleCollisionBox self, Vector3dm vec) {
        return vec != null && vec.getX() >= self.minX && vec.getX() <= self.maxX && vec.getY() >= self.minY && vec.getY() <= self.maxY;
    }

    // Look vector accounting for optifine FastMath, and client version differences
    public static Vector3dm getLook(GrimPlayer player, float yaw, float pitch) {
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_12_2)) {
            float f = player.trigHandler.cos(GrimMath.radians(-yaw) - (float) Math.PI);
            float f1 = player.trigHandler.sin(GrimMath.radians(-yaw) - (float) Math.PI);
            float f2 = -player.trigHandler.cos(GrimMath.radians(-pitch));
            float f3 = player.trigHandler.sin(GrimMath.radians(-pitch));
            return new Vector3dm(f1 * f2, f3, f * f2);
        } else {
            float f = GrimMath.radians(pitch);
            float f1 = GrimMath.radians(-yaw);
            float f2 = player.trigHandler.cos(f1);
            float f3 = player.trigHandler.sin(f1);
            float f4 = player.trigHandler.cos(f);
            float f5 = player.trigHandler.sin(f);
            return new Vector3dm(f3 * f4, -f5, (double) (f2 * f4));
        }
    }

    public static boolean isVecInside(SimpleCollisionBox self, Vector3dm vec) {
        return vec.getX() > self.minX && vec.getX() < self.maxX && (vec.getY() > self.minY && vec.getY() < self.maxY && vec.getZ() > self.minZ && vec.getZ() < self.maxZ);
    }

    public static double getMinReachToBox(GrimPlayer player, SimpleCollisionBox targetBox) {
        boolean giveMovementThresholdLenience = !player.packetStateData.didLastMovementIncludePosition || player.canSkipTicks();
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) targetBox.expand(0.1);

        double lowest = Double.MAX_VALUE;

        if (giveMovementThresholdLenience) targetBox.expand(player.getMovementThreshold());
        final double[] possibleEyeHeights = player.getPossibleEyeHeights();
        for (double eyes : possibleEyeHeights) {
            Vector3dm from = new Vector3dm(player.x, player.y + eyes, player.z);
            Vector3dm closestPoint = VectorUtils.cutBoxToVector(from, targetBox);
            lowest = Math.min(lowest, closestPoint.distance(from));
        }

        return lowest;
    }
}
