package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.Vector3dm;
import ac.grim.grimac.utils.math.VectorUtils;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class ReachUtils {
    // Copied from 1.8... I couldn't figure out 1.14+. "Enterprise" java code is unreadable!
    @Contract("_, _, _ -> new")
    public static @NotNull Pair<@Nullable Vector3dm, @Nullable BlockFace> calculateIntercept(@NotNull SimpleCollisionBox self, @NotNull Vector3dm origin, @NotNull Vector3dm end) {
        Vector3dm minX = getIntermediateWithXValue(origin, end, self.minX);
        Vector3dm maxX = getIntermediateWithXValue(origin, end, self.maxX);
        Vector3dm minY = getIntermediateWithYValue(origin, end, self.minY);
        Vector3dm maxY = getIntermediateWithYValue(origin, end, self.maxY);
        Vector3dm minZ = getIntermediateWithZValue(origin, end, self.minZ);
        Vector3dm maxZ = getIntermediateWithZValue(origin, end, self.maxZ);

        if (!isVecInYZ(self, minX)) minX = null;
        if (!isVecInYZ(self, maxX)) maxX = null;
        if (!isVecInXZ(self, minY)) minY = null;
        if (!isVecInXZ(self, maxY)) maxY = null;
        if (!isVecInXY(self, minZ)) minZ = null;
        if (!isVecInXY(self, maxZ)) maxZ = null;

        Vector3dm best = null;
        BlockFace bestFace = null;

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
    public static @Nullable Vector3dm getIntermediateWithXValue(@NotNull Vector3dm self, @NotNull Vector3dm other, double x) {
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
    public static @Nullable Vector3dm getIntermediateWithYValue(@NotNull Vector3dm self, @NotNull Vector3dm other, double y) {
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
    public static @Nullable Vector3dm getIntermediateWithZValue(@NotNull Vector3dm self, @NotNull Vector3dm other, double z) {
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
    @Contract("_, null -> false")
    private static boolean isVecInYZ(@NotNull SimpleCollisionBox self, @Nullable Vector3dm vec) {
        return vec != null && vec.getY() >= self.minY && vec.getY() <= self.maxY && vec.getZ() >= self.minZ && vec.getZ() <= self.maxZ;
    }

    /**
     * Checks if the specified vector is within the XZ dimensions of the bounding box. Args: Vec3D
     */
    @Contract("_, null -> false")
    private static boolean isVecInXZ(@NotNull SimpleCollisionBox self, @Nullable Vector3dm vec) {
        return vec != null && vec.getX() >= self.minX && vec.getX() <= self.maxX && vec.getZ() >= self.minZ && vec.getZ() <= self.maxZ;
    }

    /**
     * Checks if the specified vector is within the XY dimensions of the bounding box. Args: Vec3D
     */
    @Contract("_, null -> false")
    private static boolean isVecInXY(@NotNull SimpleCollisionBox self, @Nullable Vector3dm vec) {
        return vec != null && vec.getX() >= self.minX && vec.getX() <= self.maxX && vec.getY() >= self.minY && vec.getY() <= self.maxY;
    }

    // Look vector accounting for optifine FastMath, and client version differences
    @Contract("_, _, _ -> new")
    public static @NotNull Vector3dm getLook(@NotNull GrimPlayer player, float yaw, float pitch) {
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_12_2)) {
            float yawRadians = GrimMath.radians(-yaw) - (float) Math.PI;
            float pitchRadians = GrimMath.radians(-pitch);
            float pitchCos = -player.trigHandler.cos(pitchRadians);
            float x = player.trigHandler.sin(yawRadians);
            float y = player.trigHandler.sin(pitchRadians);
            float z = player.trigHandler.cos(yawRadians);
            return new Vector3dm(x * pitchCos, y, z * pitchCos);
        } else {
            float pitchRadians = GrimMath.radians(pitch);
            float yawRadians = GrimMath.radians(-yaw);
            float pitchCos = player.trigHandler.cos(pitchRadians);
            float x = player.trigHandler.sin(yawRadians);
            float y = player.trigHandler.sin(pitchRadians);
            float z = player.trigHandler.cos(yawRadians);
            return new Vector3dm(x * pitchCos, -y, z * pitchCos);
        }
    }

    public static boolean isVecInside(@NotNull SimpleCollisionBox self, @NotNull Vector3dm vec) {
        return vec.getX() > self.minX && vec.getX() < self.maxX && (vec.getY() > self.minY && vec.getY() < self.maxY && vec.getZ() > self.minZ && vec.getZ() < self.maxZ);
    }

    public static double getMinReachToBox(@NotNull GrimPlayer player, @NotNull SimpleCollisionBox targetBox) {
        double lowest = Double.MAX_VALUE;

        final double[] possibleEyeHeights = player.getPossibleEyeHeights();
        for (double eyes : possibleEyeHeights) {
            Vector3dm closestPoint = VectorUtils.cutBoxToVector(player.x, player.y + eyes, player.z, targetBox);
            lowest = Math.min(lowest, closestPoint.distance(player.x, player.y + eyes, player.z));
        }

        return lowest;
    }
}
