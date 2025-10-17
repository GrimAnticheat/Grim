package ac.grim.grimac.predictionengine.blockeffects;

import ac.grim.grimac.utils.nmsutil.Collisions;
import com.github.retrooper.packetevents.protocol.world.Direction;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import com.google.common.collect.ImmutableList;
import lombok.experimental.UtilityClass;

import java.util.Optional;

@UtilityClass
public class BlockCollisions {

    public static final Vector3d X_AXIS = new Vector3d(1.0, 0.0, 0.0);
    public static final Vector3d Y_AXIS = new Vector3d(0.0, 1.0, 0.0);
    public static final Vector3d Z_AXIS = new Vector3d(0.0, 0.0, 1.0);

    public static Vector3i getFurthestCorner(Vector3d vector) {
        double xDot = Math.abs(X_AXIS.dot(vector));
        double yDot = Math.abs(Y_AXIS.dot(vector));
        double zDot = Math.abs(Z_AXIS.dot(vector));
        int xSign = vector.x >= 0.0 ? 1 : -1;
        int ySign = vector.y >= 0.0 ? 1 : -1;
        int zSign = vector.z >= 0.0 ? 1 : -1;
        if (xDot <= yDot && xDot <= zDot) {
            return new Vector3i(-xSign, -zSign, ySign);
        } else {
            return yDot <= zDot ? new Vector3i(zSign, -ySign, -xSign) : new Vector3i(-ySign, xSign, -zSign);
        }
    }

    public static Optional<Vector3d> clip(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Vector3d start, Vector3d end) {
        double[] minDistance = new double[]{1.0};
        double deltaX = end.x - start.x;
        double deltaY = end.y - start.y;
        double deltaZ = end.z - start.z;
        Direction direction = getDirection(minX, minY, minZ, maxX, maxY, maxZ, start, minDistance, null, deltaX, deltaY, deltaZ);
        if (direction == null) {
            return Optional.empty();
        } else {
            double distance = minDistance[0];
            return Optional.of(start.add(distance * deltaX, distance * deltaY, distance * deltaZ));
        }
    }

    private static Direction getDirection(
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ,
            Vector3d start,
            double[] minDistance,
            Direction facing,
            double deltaX,
            double deltaY,
            double deltaZ
    ) {
        if (deltaX > Collisions.COLLISION_EPSILON) {
            facing = clipPoint(minDistance, facing, deltaX, deltaY, deltaZ, minX, minY, maxY, minZ, maxZ, Direction.WEST, start.x, start.y, start.z);
        } else if (deltaX < -Collisions.COLLISION_EPSILON) {
            facing = clipPoint(minDistance, facing, deltaX, deltaY, deltaZ, maxX, minY, maxY, minZ, maxZ, Direction.EAST, start.x, start.y, start.z);
        }

        if (deltaY > Collisions.COLLISION_EPSILON) {
            facing = clipPoint(minDistance, facing, deltaY, deltaZ, deltaX, minY, minZ, maxZ, minX, maxX, Direction.DOWN, start.y, start.z, start.x);
        } else if (deltaY < -Collisions.COLLISION_EPSILON) {
            facing = clipPoint(minDistance, facing, deltaY, deltaZ, deltaX, maxY, minZ, maxZ, minX, maxX, Direction.UP, start.y, start.z, start.x);
        }

        if (deltaZ > Collisions.COLLISION_EPSILON) {
            facing = clipPoint(minDistance, facing, deltaZ, deltaX, deltaY, minZ, minX, maxX, minY, maxY, Direction.NORTH, start.z, start.x, start.y);
        } else if (deltaZ < -Collisions.COLLISION_EPSILON) {
            facing = clipPoint(minDistance, facing, deltaZ, deltaX, deltaY, maxZ, minX, maxX, minY, maxY, Direction.SOUTH, start.z, start.x, start.y);
        }

        return facing;
    }

    public static Direction clipPoint(
            double[] minDistance,
            Direction prevDirection,
            double distanceSide,
            double distanceOtherA,
            double distanceOtherB,
            double minSide,
            double minOtherA,
            double maxOtherA,
            double minOtherB,
            double maxOtherB,
            Direction hitSide,
            double startSide,
            double startOtherA,
            double startOtherB
    ) {
        double sideDistance = (minSide - startSide) / distanceSide;
        double otherDistanceA = startOtherA + sideDistance * distanceOtherA;
        double otherDistanceB = startOtherB + sideDistance * distanceOtherB;
        if (sideDistance > 0.0 && sideDistance < minDistance[0]
                && minOtherA - Collisions.COLLISION_EPSILON < otherDistanceA
                && otherDistanceA < maxOtherA + Collisions.COLLISION_EPSILON
                && minOtherB - Collisions.COLLISION_EPSILON < otherDistanceB
                && otherDistanceB < maxOtherB + Collisions.COLLISION_EPSILON) {
            minDistance[0] = sideDistance;
            return hitSide;
        } else {
            return prevDirection;
        }
    }

    public static final ImmutableList<Collisions.Axis> YXZ_AXIS_ORDER = ImmutableList.of(Collisions.Axis.Y, Collisions.Axis.X, Collisions.Axis.Z);
    public static final ImmutableList<Collisions.Axis> YZX_AXIS_ORDER = ImmutableList.of(Collisions.Axis.Y, Collisions.Axis.Z, Collisions.Axis.X);

    public static ImmutableList<Collisions.Axis> axisStepOrder(Vector3d vector) {
        return Math.abs(vector.getX()) < Math.abs(vector.getZ()) ? YZX_AXIS_ORDER : YXZ_AXIS_ORDER;
    }

    public static Vector3d relative(Vector3d curr, Direction direction, double value) {
        Vector3i vec = direction.getVector();
        return new Vector3d(
                curr.x + value * vec.getX(), curr.y + value * vec.getY(), curr.z + value * vec.getZ()
        );
    }

}
