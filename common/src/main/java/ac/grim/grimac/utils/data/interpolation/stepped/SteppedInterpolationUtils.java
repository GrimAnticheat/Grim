package ac.grim.grimac.utils.data.interpolation.stepped;

import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import com.github.retrooper.packetevents.util.Vector3d;

public final class SteppedInterpolationUtils {

    public static SimpleCollisionBox pointBounds(Vector3d position) {
        return new SimpleCollisionBox(position.x, position.y, position.z, position.x, position.y, position.z, false);
    }

    public static boolean isPoint(SimpleCollisionBox bounds) {
        return bounds.minX == bounds.maxX && bounds.minY == bounds.maxY && bounds.minZ == bounds.maxZ;
    }

    public static void setPointBounds(SimpleCollisionBox bounds, double positionX, double positionY, double positionZ) {
        bounds.minX = bounds.maxX = positionX;
        bounds.minY = bounds.maxY = positionY;
        bounds.minZ = bounds.maxZ = positionZ;
    }

    public static void copyBounds(SimpleCollisionBox source, SimpleCollisionBox destination) {
        destination.minX = source.minX;
        destination.minY = source.minY;
        destination.minZ = source.minZ;
        destination.maxX = source.maxX;
        destination.maxY = source.maxY;
        destination.maxZ = source.maxZ;
    }

    public static void clearBounds(SimpleCollisionBox bounds) {
        bounds.minX = bounds.minY = bounds.minZ = Double.POSITIVE_INFINITY;
        bounds.maxX = bounds.maxY = bounds.maxZ = Double.NEGATIVE_INFINITY;
    }

    public static void expandByTranslation(SimpleCollisionBox bounds, SimpleCollisionBox translationBounds) {
        bounds.minX += translationBounds.minX;
        bounds.minY += translationBounds.minY;
        bounds.minZ += translationBounds.minZ;
        bounds.maxX += translationBounds.maxX;
        bounds.maxY += translationBounds.maxY;
        bounds.maxZ += translationBounds.maxZ;
    }

    public static void encompassInterpolatedBounds(SimpleCollisionBox result, SimpleCollisionBox start, SimpleCollisionBox end, float progress) {
        result.encompass(
                start.minX + progress * (end.minX - start.minX),
                start.minY + progress * (end.minY - start.minY),
                start.minZ + progress * (end.minZ - start.minZ)
        );

        result.encompass(
                start.maxX + progress * (end.maxX - start.maxX),
                start.maxY + progress * (end.maxY - start.maxY),
                start.maxZ + progress * (end.maxZ - start.maxZ)
        );
    }

    public static float segmentProgress(double pathProgress, double segmentStart, int durationTicks) {
        // A step with no duration is finished as soon as the client receives it
        if (durationTicks == 0) {
            return 1;
        }

        return (float) Math.max(0, Math.min(1, (pathProgress - segmentStart) / durationTicks));
    }

    public static double maxDistanceSquared(SimpleCollisionBox bounds, Vector3d position) {
        double distanceX = Math.max(Math.abs(bounds.minX - position.x), Math.abs(bounds.maxX - position.x));
        double distanceY = Math.max(Math.abs(bounds.minY - position.y), Math.abs(bounds.maxY - position.y));
        double distanceZ = Math.max(Math.abs(bounds.minZ - position.z), Math.abs(bounds.maxZ - position.z));
        return distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ;
    }

    public static float lerp(float progress, float start, float end) {
        return start + progress * (end - start);
    }

    public static float lerpYaw(float progress, float startYaw, float endYaw) {
        float yawDifference = (endYaw - startYaw) % 360.0F;
        if (yawDifference >= 180.0F) {
            yawDifference -= 360.0F;
        }

        if (yawDifference < -180.0F) {
            yawDifference += 360.0F;
        }

        return startYaw + progress * yawDifference;
    }

}
