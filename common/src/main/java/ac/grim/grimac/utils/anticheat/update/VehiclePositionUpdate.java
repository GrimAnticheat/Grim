package ac.grim.grimac.utils.anticheat.update;

import com.github.retrooper.packetevents.util.Vector3d;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class VehiclePositionUpdate {
    private final Vector3d from, to;
    private final float xRot, yRot;
    private final boolean isTeleport;
}
