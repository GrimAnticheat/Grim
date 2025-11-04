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
    public static @NotNull Pair<@Nullable Vector3dm, @Nullable BlockFace> calculateIntercept(@NotNull SimpleCollisionBox self,
                                                                                             double originX, double originY, double originZ,
                                                                                             double endX, double endY, double endZ) {
        Vector3dm minX = getIntermediateWithXValue(originX, originY, originZ, endX, endY, endZ, self.minX);
        Vector3dm maxX = getIntermediateWithXValue(originX, originY, originZ, endX, endY, endZ, self.maxX);
        Vector3dm minY = getIntermediateWithYValue(originX, originY, originZ, endX, endY, endZ, self.minY);
        Vector3dm maxY = getIntermediateWithYValue(originX, originY, originZ, endX, endY, endZ, self.maxY);
        Vector3dm minZ = getIntermediateWithZValue(originX, originY, originZ, endX, endY, endZ, self.minZ);
        Vector3dm maxZ = getIntermediateWithZValue(originX, originY, originZ, endX, endY, endZ, self.maxZ);

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

        if (maxX != null && (best == null || maxX.distanceSquared(originX, originY, originZ) < best.distanceSquared(originX, originY, originZ))) {
            best = maxX;
            bestFace = BlockFace.EAST;
        }

        if (minY != null && (best == null || minY.distanceSquared(originX, originY, originZ) < best.distanceSquared(originX, originY, originZ))) {
            best = minY;
            bestFace = BlockFace.DOWN;
        }

        if (maxY != null && (best == null || maxY.distanceSquared(originX, originY, originZ) < best.distanceSquared(originX, originY, originZ))) {
            best = maxY;
            bestFace = BlockFace.UP;
        }

        if (minZ != null && (best == null || minZ.distanceSquared(originX, originY, originZ) < best.distanceSquared(originX, originY, originZ))) {
            best = minZ;
            bestFace = BlockFace.NORTH;
        }

        if (maxZ != null && (best == null || maxZ.distanceSquared(originX, originY, originZ) < best.distanceSquared(originX, originY, originZ))) {
            best = maxZ;
            bestFace = BlockFace.SOUTH;
        }

        return new Pair<>(best, bestFace);
    }

    /**
     * Returns a new vector with x value equal to the second parameter, along the line between this vector and the
     * passed in vector, or null if not possible.
     */
    public static @Nullable Vector3dm getIntermediateWithXValue(
            double originX, double originY, double originZ,
            double endX, double endY, double endZ,
            double targetX
    ) {
        double deltaX = endX - originX;
        double deltaY = endY - originY;
        double deltaZ = endZ - originZ;

        if (deltaX * deltaX < 1.0000000116860974E-7D) {
            return null;
        } else {
            double d3 = (targetX - originX) / deltaX;
            return d3 >= 0.0D && d3 <= 1.0D ? new Vector3dm(originX + deltaX * d3, originY + deltaY * d3, originZ + deltaZ * d3) : null;
        }
    }

    /**
     * Returns a new vector with y value equal to the second parameter, along the line between this vector and the
     * passed in vector, or null if not possible.
     */
    public static @Nullable Vector3dm getIntermediateWithYValue(
            double originX, double originY, double originZ,
            double endX, double endY, double endZ,
            double targetY
    ) {
        double deltaX = endX - originX;
        double deltaY = endY - originY;
        double deltaZ = endZ - originZ;

        if (deltaY * deltaY < 1.0000000116860974E-7D) {
            return null;
        } else {
            double d3 = (targetY - originY) / deltaY;
            return d3 >= 0.0D && d3 <= 1.0D ? new Vector3dm(originX + deltaX * d3, originY + deltaY * d3, originZ + deltaZ * d3) : null;
        }
    }

    /**
     * Returns a new vector with z value equal to the second parameter, along the line between this vector and the
     * passed in vector, or null if not possible.
     */
    public static @Nullable Vector3dm getIntermediateWithZValue(
            double originX, double originY, double originZ,
            double endX, double endY, double endZ,
            double targetZ
    ) {
        double deltaX = endX - originX;
        double deltaY = endY - originY;
        double deltaZ = endZ - originZ;

        if (deltaZ * deltaZ < 1.0000000116860974E-7D) {
            return null;
        } else {
            double d3 = (targetZ - originZ) / deltaZ;
            return d3 >= 0.0D && d3 <= 1.0D ? new Vector3dm(originX + deltaX * d3, originY + deltaY * d3, originZ + deltaZ * d3) : null;
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

    public static boolean isVecInside(@NotNull SimpleCollisionBox self, double x, double y, double z) {
        return x > self.minX && x < self.maxX && y > self.minY && y < self.maxY && z > self.minZ && z < self.maxZ;
    }

    public static double getMinReachToBox(@NotNull GrimPlayer player, @NotNull SimpleCollisionBox targetBox) {
        boolean giveMovementThresholdLenience = !player.packetStateData.didLastMovementIncludePosition || player.canSkipTicks();
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8))
            targetBox.expand(0.1);

        double lowest = Double.MAX_VALUE;

        if (giveMovementThresholdLenience) targetBox.expand(player.getMovementThreshold());
        final double[] possibleEyeHeights = player.getPossibleEyeHeights();
        for (double eyes : possibleEyeHeights) {
            Vector3dm closestPoint = VectorUtils.cutBoxToVector(player.x, player.y + eyes, player.z, targetBox);
            lowest = Math.min(lowest, closestPoint.distance(player.x, player.y + eyes, player.z));
        }

        return lowest;
    }
}
