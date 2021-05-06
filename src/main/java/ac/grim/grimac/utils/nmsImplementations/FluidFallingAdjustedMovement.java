package ac.grim.grimac.utils.nmsImplementations;

import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.util.Vector;

public class FluidFallingAdjustedMovement {
    // LivingEntity line 1882
    public static Vector getFluidFallingAdjustedMovement(GrimPlayer player, double d, boolean bl, Vector vec3) {
        // Removed gravity reference because players always have gravity
        if (!player.isSprinting) {
            double d2 = bl && Math.abs(vec3.getY() - 0.005) >= 0.003 && Math.abs(vec3.getY() - d / 16.0) < 0.003 ? -0.003 : vec3.getY() - d / 16.0;
            return new Vector(vec3.getX(), d2, vec3.getZ());
        }
        return vec3;
    }
}
