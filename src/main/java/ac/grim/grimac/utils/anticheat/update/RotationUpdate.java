package ac.grim.grimac.utils.anticheat.update;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public final class RotationUpdate {
    private float lastPitch, lastYaw, pitch, yaw, deltaPitch, deltaYaw;
}
