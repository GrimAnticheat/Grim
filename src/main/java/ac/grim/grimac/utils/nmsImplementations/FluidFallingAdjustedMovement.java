package ac.grim.grimac.utils.nmsImplementations;

import ac.grim.grimac.GrimPlayer;
import org.bukkit.util.Vector;

public class FluidFallingAdjustedMovement {
    // LivingEntity line 1882
    public static Vector getFluidFallingAdjustedMovement(GrimPlayer grimPlayer, double d, boolean bl, Vector vec3) {
        if (grimPlayer.bukkitPlayer.hasGravity() && !grimPlayer.bukkitPlayer.isSprinting()) {
            double d2 = bl && Math.abs(vec3.getY() - 0.005) >= 0.003 && Math.abs(vec3.getY() - d / 16.0) < 0.003 ? -0.003 : vec3.getY() - d / 16.0;
            return new Vector(vec3.getX(), d2, vec3.getZ());
        }
        return vec3;
    }
}
