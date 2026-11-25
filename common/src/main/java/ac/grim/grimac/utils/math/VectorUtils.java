package ac.grim.grimac.utils.math;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.util.Vector3d;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class VectorUtils {
    public static @NotNull Vector3dm cutBoxToVector(@NotNull Vector3dm vectorToCutTo, @NotNull Vector3dm min, @NotNull Vector3dm max) {
        SimpleCollisionBox box = new SimpleCollisionBox(min, max).sort();
        return cutBoxToVector(vectorToCutTo, box);
    }

    @Contract("_, _ -> new")
    public static @NotNull Vector3dm cutBoxToVector(@NotNull Vector3dm vectorCutTo, @NotNull SimpleCollisionBox box) {
        return cutBoxToVector(vectorCutTo.getX(), vectorCutTo.getY(), vectorCutTo.getZ(), box);
    }

    public static @NotNull Vector3dm cutBoxToVector(double x, double y, double z, @NotNull SimpleCollisionBox box) {
        return new Vector3dm(GrimMath.clamp(x, box.minX, box.maxX),
                GrimMath.clamp(y, box.minY, box.maxY),
                GrimMath.clamp(z, box.minZ, box.maxZ));
    }

    @Contract("_ -> new")
    public static @NotNull Vector3dm fromVec3d(@NotNull Vector3d vector3d) {
        return new Vector3dm(vector3d.getX(), vector3d.getY(), vector3d.getZ());
    }

    // Clamping stops the player from causing an integer overflow and crashing the Netty thread
    @Contract("_ -> new")
    public static @NotNull Vector3d clampVector(@NotNull Vector3d toClamp) {
        return clampVector(toClamp.getX(), toClamp.getY(), toClamp.getZ());
    }

    // Clamping stops the player from causing an integer overflow and crashing the Netty thread
    @Contract("_, _, _ -> new")
    public static @NotNull Vector3d clampVector(double x, double y, double z) {
        double newX = GrimMath.clamp(x, -3.0E7D, 3.0E7D);
        double newY = GrimMath.clamp(y, -2.0E7D, 2.0E7D);
        double newZ = GrimMath.clamp(z, -3.0E7D, 3.0E7D);

        return new Vector3d(newX, newY, newZ);
    }

    public static Vector3dm normalize(GrimPlayer player, Vector3dm vec) {
        return normalize(player.getClientVersion(), vec);
    }

    public static Vector3dm normalize(ClientVersion version, Vector3dm vec) {
        double d0 = getVanillaLength(version, vec);
        return version.isNewerThanOrEquals(ClientVersion.V_1_21_2) ? modern$normalize(vec, d0) : legacy$normalize(vec, d0);
    }

    public static double getVanillaLength(ClientVersion version, Vector3dm vec) {
        return getVanillaLength(version, vec.getX(), vec.getY(), vec.getZ());
    }

    public static double getVanillaLength(ClientVersion version, double x, double y, double z) {
        double lengthSquared = x * x + y * y + z * z;
        return version.isOlderThan(ClientVersion.V_1_17) ? (float) Math.sqrt(lengthSquared) : Math.sqrt(lengthSquared);
    }

    private static Vector3dm legacy$normalize(Vector3dm vec, double d0) {
        return d0 < 1.0E-4D ? new Vector3dm() : new Vector3dm(vec.getX() / d0, vec.getY() / d0, vec.getZ() / d0);
    }

    private static Vector3dm modern$normalize(Vector3dm vec, double d0) {
        return d0 < 1.0E-5F ? new Vector3dm() : new Vector3dm(vec.getX() / d0, vec.getY() / d0, vec.getZ() / d0);
    }

    @Contract(pure = true)
    public static double distanceSquared(@NotNull Vector3d vec1, @NotNull Vector3d vec2) {
        return distanceSquared(vec1.getX(), vec1.getY(), vec1.getZ(), vec2.getX(), vec2.getY(), vec2.getZ());
    }

    @Contract(pure = true)
    public static double distanceSquared(@NotNull Vector3d vec1, @NotNull Vector3dm vec2) {
        return distanceSquared(vec1.getX(), vec1.getY(), vec1.getZ(), vec2.getX(), vec2.getY(), vec2.getZ());
    }

    @Contract(pure = true)
    public static double distanceSquared(@NotNull Vector3dm vec1, @NotNull Vector3d vec2) {
        return distanceSquared(vec1.getX(), vec1.getY(), vec1.getZ(), vec2.getX(), vec2.getY(), vec2.getZ());
    }

    @Contract(pure = true)
    public static double distanceSquared(@NotNull Vector3dm vec1, @NotNull Vector3dm vec2) {
        return distanceSquared(vec1.getX(), vec1.getY(), vec1.getZ(), vec2.getX(), vec2.getY(), vec2.getZ());
    }

    @Contract(pure = true)
    public static double distanceSquared(double x1, double y1, double z1, @NotNull Vector3d vec2) {
        return distanceSquared(x1, y1, z1, vec2.getX(), vec2.getY(), vec2.getZ());
    }

    @Contract(pure = true)
    public static double distanceSquared(double x1, double y1, double z1, @NotNull Vector3dm vec2) {
        return distanceSquared(x1, y1, z1, vec2.getX(), vec2.getY(), vec2.getZ());
    }

    @Contract(pure = true)
    public static double distanceSquared(@NotNull Vector3d vec1, double x2, double y2, double z2) {
        return distanceSquared(vec1.getX(), vec1.getY(), vec1.getZ(), x2, y2, z2);
    }

    @Contract(pure = true)
    public static double distanceSquared(@NotNull Vector3dm vec1, double x2, double y2, double z2) {
        return distanceSquared(vec1.getX(), vec1.getY(), vec1.getZ(), x2, y2, z2);
    }

    @Contract(pure = true)
    public static double distanceSquared(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return dx * dx + dy * dy + dz * dz;
    }

    @Contract(pure = true)
    public static double distance(@NotNull Vector3d vec1, @NotNull Vector3d vec2) {
        return distance(vec1.getX(), vec1.getY(), vec1.getZ(), vec2.getX(), vec2.getY(), vec2.getZ());
    }

    @Contract(pure = true)
    public static double distance(double x1, double y1, double z1, @NotNull Vector3d vec2) {
        return distance(x1, y1, z1, vec2.getX(), vec2.getY(), vec2.getZ());
    }

    @Contract(pure = true)
    public static double distance(@NotNull Vector3d vec1, double x2, double y2, double z2) {
        return distance(vec1.getX(), vec1.getY(), vec1.getZ(), x2, y2, z2);
    }

    @Contract(pure = true)
    public static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Math.sqrt(distanceSquared(x1, y1, z1, x2, y2, z2));
    }
}
