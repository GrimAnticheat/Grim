package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.Vector3dm;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class FluidFallingAdjustedMovement {
    public static double getAdjustedY(GrimPlayer player, double gravity, boolean isFalling, double currentY) {
        if (!player.hasGravity || player.isSprinting) return currentY;
        isFalling = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14) ? isFalling : currentY < 0;
        return isFalling && Math.abs(currentY - 0.005) >= 0.003 && Math.abs(currentY - gravity / 16.0) < 0.003 ? -0.003 : currentY - gravity / 16.0;
    }
}
