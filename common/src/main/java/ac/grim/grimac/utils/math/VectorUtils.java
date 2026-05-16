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

    // Clamping stops the player from causing an integer overflow and crashing the netty thread
    @Contract("_ -> new")
    public static @NotNull Vector3d clampVector(@NotNull Vector3d toClamp) {
        double x = GrimMath.clamp(toClamp.getX(), -3.0E7D, 3.0E7D);
        double y = GrimMath.clamp(toClamp.getY(), -2.0E7D, 2.0E7D);
        double z = GrimMath.clamp(toClamp.getZ(), -3.0E7D, 3.0E7D);

        return new Vector3d(x, y, z);
    }

    public static Vector3dm normalize(GrimPlayer player, Vector3dm vec) {
        return normalize(player.getClientVersion(), vec);
    }

    public static Vector3dm normalize(ClientVersion version, Vector3dm vec) {
        double d0 = Math.sqrt(vec.getX() * vec.getX() + vec.getY() * vec.getY() + vec.getZ() * vec.getZ());
        return version.isNewerThanOrEquals(ClientVersion.V_1_21_2) ? modern$normalize(vec, d0) : legacy$normalize(vec, d0);
    }

    private static Vector3dm legacy$normalize(Vector3dm vec, double d0) {
        return d0 < 1.0E-4D ? new Vector3dm() : new Vector3dm(vec.getX() / d0, vec.getY() / d0, vec.getZ() / d0);
    }

    private static Vector3dm modern$normalize(Vector3dm vec, double d0) {
        return d0 < 1.0E-5F ? new Vector3dm() : new Vector3dm(vec.getX() / d0, vec.getY() / d0, vec.getZ() / d0);
    }

}
