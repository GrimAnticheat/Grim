package ac.grim.grimac.utils.nmsutil;


import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.Pair;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import org.bukkit.util.Vector;

public class ReachUtils {
    // Copied from 1.8... I couldn't figure out 1.14+. "Enterprise" java code is unreadable!
    public static Pair<Vector, BlockFace> calculateIntercept(SimpleCollisionBox self, Vector origin, Vector end) {
        Vector minX = getIntermediateWithXValue(origin, end, self.minX);
        Vector maxX = getIntermediateWithXValue(origin, end, self.maxX);
        Vector minY = getIntermediateWithYValue(origin, end, self.minY);
        Vector maxY = getIntermediateWithYValue(origin, end, self.maxY);
        Vector minZ = getIntermediateWithZValue(origin, end, self.minZ);
        Vector maxZ = getIntermediateWithZValue(origin, end, self.maxZ);

        BlockFace bestFace = null;

        if (!isVecInYZ(self, minX)) {
            minX = null;
        }

        if (!isVecInYZ(self, maxX)) {
            maxX = null;
        }

        if (!isVecInXZ(self, minY)) {
            minY = null;
        }

        if (!isVecInXZ(self, maxY)) {
            maxY = null;
        }

        if (!isVecInXY(self, minZ)) {
            minZ = null;
        }

        if (!isVecInXY(self, maxZ)) {
            maxZ = null;
        }

        Vector best = null;

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

    // Look vector accounting for optifine FastMath, and client version differences
    public static Vector getLook(GrimPlayer player, float yaw, float pitch) {
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_12_2)) {
            float f = player.trigHandler.cos(-yaw * 0.017453292F - (float)Math.PI);
            float f1 = player.trigHandler.sin(-yaw * 0.017453292F - (float)Math.PI);
            float f2 = -player.trigHandler.cos(-pitch * 0.017453292F);
            float f3 = player.trigHandler.sin(-pitch * 0.017453292F);
            return new Vector(f1 * f2, f3, f * f2);
        } else {
            float f = pitch * ((float) Math.PI / 180F);
            float f1 = -yaw * ((float) Math.PI / 180F);
            float f2 = player.trigHandler.cos(f1);
            float f3 = player.trigHandler.sin(f1);
            float f4 = player.trigHandler.cos(f);
            float f5 = player.trigHandler.sin(f);
            return new Vector(f3 * f4, -f5, (double) (f2 * f4));
        }
    }

    public static boolean isVecInside(SimpleCollisionBox self, Vector vec) {
        return vec.getX() > self.minX && vec.getX() < self.maxX && (vec.getY() > self.minY && vec.getY() < self.maxY && vec.getZ() > self.minZ && vec.getZ() < self.maxZ);
    }
}
