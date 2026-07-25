package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.Vector3dm;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@UtilityClass
public class FluidFallingAdjustedMovement {
    public static @NotNull Vector3dm getFluidFallingAdjustedMovement(@NotNull GrimPlayer player, double gravity, boolean isFalling, @NotNull Vector3dm velocity) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(velocity, "velocity");

        if (!player.hasGravity || player.isSprinting) return velocity;
        isFalling = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14) ? isFalling : velocity.getY() < 0;
        double newY = isFalling && Math.abs(velocity.getY() - 0.005) >= 0.003 && Math.abs(velocity.getY() - gravity / 16.0) < 0.003 ? -0.003 : velocity.getY() - gravity / 16.0;
        return new Vector3dm(velocity.getX(), newY, velocity.getZ());
    }
}
