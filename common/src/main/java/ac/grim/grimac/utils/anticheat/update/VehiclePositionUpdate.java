package ac.grim.grimac.utils.anticheat.update;

import com.github.retrooper.packetevents.util.Vector3d;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record VehiclePositionUpdate(
        @NotNull Vector3d from,
        @NotNull Vector3d to,
        float yaw,
        float pitch,
        boolean onGround,
        boolean isTeleport
) {
    public VehiclePositionUpdate {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
    }
}
