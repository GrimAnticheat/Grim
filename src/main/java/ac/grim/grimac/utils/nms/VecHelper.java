package ac.grim.grimac.utils.nms;

import net.minecraft.server.v1_16_R3.Vec3D;
import org.bukkit.util.Vector;

// <removed rant about spigot mappings>
// If I add 1.12 support...
public class VecHelper {
    public static Vector mojangToBukkit(Vec3D vec3D) {
        return new Vector(vec3D.x, vec3D.y, vec3D.z);
    }

    public static Vec3D bukkitToMojang(Vector vector) {
        return new Vec3D(vector.getX(), vector.getY(), vector.getZ());
    }
}
