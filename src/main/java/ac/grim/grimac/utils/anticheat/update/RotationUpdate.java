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
    private float deltaYRot, deltaXRot;
    private boolean isCinematic;
    private double sensitivityX, sensitivityY;

    public RotationUpdate(HeadRotation from, HeadRotation to, float deltaPitch, float deltaXRot) {
        this.from = from;
        this.to = to;
        this.deltaYRot = deltaPitch;
        this.deltaXRot = deltaXRot;
    }

    public float getDeltaXRotABS() {
        return Math.abs(getDeltaXRot());
    }

    public float getDeltaYRotABS() {
        return Math.abs(getDeltaYRot());
    }
}
