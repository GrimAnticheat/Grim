package ac.grim.grimac.utils.anticheat.update;

import ac.grim.grimac.checks.impl.aim.processor.AimProcessor;
import ac.grim.grimac.utils.data.HeadRotation;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class RotationUpdate {
    private HeadRotation from, to;
    private AimProcessor processor;
    private float deltaPitch, deltaYaw;
    private boolean isCinematic;
    private double sensitivityX, sensitivityY;

    public RotationUpdate(HeadRotation from, HeadRotation to, float deltaPitch, float deltaYaw) {
        this.from = from;
        this.to = to;
        this.deltaPitch = deltaPitch;
        this.deltaYaw = deltaYaw;
    }

    // TODO: Math.abs stuff
}
