package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.math.Vector3dm;
import com.github.retrooper.packetevents.util.Vector3d;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

// Copied directly from Hawk
public record Ray(double originX, double originY, double originZ, double directionX, double directionY, double directionZ) implements Cloneable {

    @Contract(value = "_, _, _, _, _, _ -> new", pure = true)
    public static @NotNull Ray from(@NotNull GrimPlayer player, double x, double y, double z, float yaw, float pitch) {
        Vector3dm direction = ReachUtils.getLook(player, yaw, pitch);
        return new Ray(x, y, z, direction.getX(), direction.getY(), direction.getZ());
    }

    @Contract(" -> this")
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public @NotNull Ray clone() {
        return this;
    }

    @Contract(value = "_ -> new", pure = true)
    public @NotNull Vector3d getPointAtDistance(double distance) {
        double x = originX + directionX * distance;
        double y = originY + directionY * distance;
        double z = originZ + directionZ * distance;

        return new Vector3d(x, y, z);
    }

    @Contract(pure = true)
    public double getXPointAtDistance(double distance) {
        return originX + directionX * distance;
    }

    @Contract(pure = true)
    public double getYPointAtDistance(double distance) {
        return originY + directionY * distance;
    }

    @Contract(pure = true)
    public double getZPointAtDistance(double distance) {
        return originZ + directionZ * distance;
    }

    // https://en.wikipedia.org/wiki/Skew_lines#Nearest_Points
    @Contract("_ -> new")
    public @NotNull Pair<@NotNull Vector3d, @NotNull Vector3d> closestPointsBetweenLines(@NotNull Ray other) {
        double directionX = this.directionX;
        double directionY = this.directionY;
        double directionZ = this.directionZ;
        double originX = this.originX;
        double originY = this.originY;
        double originZ = this.originZ;
        double otherDirectionX = other.directionX;
        double otherDirectionY = other.directionY;
        double otherDirectionZ = other.directionZ;
        double otherOriginX = other.originX;
        double otherOriginY = other.originY;
        double otherOriginZ = other.originZ;

        double crossX1 = directionY * otherDirectionZ - otherDirectionY * directionZ;
        double crossY1 = directionZ * otherDirectionX - otherDirectionZ * directionX;
        double crossZ1 = directionX * otherDirectionY - otherDirectionX * directionY;
        double orthoX1 = otherDirectionY * crossZ1 - crossY1 * otherDirectionZ;
        double orthoY1 = otherDirectionZ * crossX1 - crossZ1 * otherDirectionX;
        double orthoZ1 = otherDirectionX * crossY1 - crossX1 * otherDirectionY;

        double numerator1 = (otherOriginX - originX) * orthoX1
                + (otherOriginY - originY) * orthoY1
                + (otherOriginZ - originZ) * orthoZ1;
        double denominator1 = directionX * orthoX1
                + directionY * orthoY1
                + directionZ * orthoZ1;
        double scale1 = numerator1 / denominator1;
        Vector3d point1 = new Vector3d(
                originX + directionX * scale1,
                originY + directionY * scale1,
                originZ + directionZ * scale1
        );

        double crossX2 = otherDirectionY * directionZ - directionY * otherDirectionZ;
        double crossY2 = otherDirectionZ * directionX - directionZ * otherDirectionX;
        double crossZ2 = otherDirectionX * directionY - directionX * otherDirectionY;
        double orthoX2 = directionY * crossZ2 - crossY2 * directionZ;
        double orthoY2 = directionZ * crossX2 - crossZ2 * directionX;
        double orthoZ2 = directionX * crossY2 - crossX2 * directionY;

        double numerator2 = (originX - otherOriginX) * orthoX2
                + (originY - otherOriginY) * orthoY2
                + (originZ - otherOriginZ) * orthoZ2;
        double denominator2 = otherDirectionX * orthoX2
                + otherDirectionY * orthoY2
                + otherDirectionZ * orthoZ2;
        double scale2 = numerator2 / denominator2;
        Vector3d point2 = new Vector3d(
                otherOriginX + otherDirectionX * scale2,
                otherOriginY + otherDirectionY * scale2,
                otherOriginZ + otherDirectionZ * scale2
        );

        return new Pair<>(point1, point2);
    }
}
