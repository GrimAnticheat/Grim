package ac.grim.grimac.utils.anticheat.update;

import ac.grim.grimac.utils.data.HeadRotation;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class RotationUpdate {
    private HeadRotation from, to;
    private float deltaPitch, deltaYaw;
    private boolean isCinematic;
    private double sensitivityX, sensitivityY;

    public RotationUpdate(HeadRotation from, HeadRotation to, float deltaPitch, float deltaYaw) {
        this.from = from;
        this.to = to;
        this.deltaPitch = deltaPitch;
        this.deltaYaw = deltaYaw;
    }
}
