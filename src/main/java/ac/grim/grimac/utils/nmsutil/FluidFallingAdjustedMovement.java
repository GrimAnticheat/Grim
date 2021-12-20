package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.bukkit.util.Vector;

public class FluidFallingAdjustedMovement {
    public static Vector getFluidFallingAdjustedMovement(GrimPlayer player, double d, boolean bl, Vector vec3) {
        if (player.hasGravity && !player.isSprinting) {
            boolean falling = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14) ? bl : vec3.getY() < 0;
            double d2 = falling && Math.abs(vec3.getY() - 0.005) >= 0.003 && Math.abs(vec3.getY() - d / 16.0) < 0.003 ? -0.003 : vec3.getY() - d / 16.0;
            return new Vector(vec3.getX(), d2, vec3.getZ());
        }
        return vec3;
    }
}
