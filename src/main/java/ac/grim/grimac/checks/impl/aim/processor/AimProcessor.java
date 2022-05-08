package ac.grim.grimac.checks.impl.aim.processor;

import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.data.HeadRotation;
import ac.grim.grimac.utils.lists.RunningMode;
import ac.grim.grimac.utils.math.GrimMath;

// From OverFlow V2 AntiCheat, modified from o(n^2) to best case o(1) worst case o(n) time.
public class AimProcessor extends RotationCheck {
    private final RunningMode<Double> yawSamples = new RunningMode<>(50);
    private final RunningMode<Double> pitchSamples = new RunningMode<>(50);
    public double sensitivityX, sensitivityY, deltaX, deltaY;
    private float lastDeltaYaw, lastDeltaPitch;

    public AimProcessor(final GrimPlayer playerData) {
        super(playerData);
    }

    private static double yawToF2(double yawDelta) {
        return yawDelta / .15;
    }

    private static double pitchToF3(double pitchDelta) {
        int b0 = pitchDelta >= 0 ? 1 : -1; //Checking for inverted mouse.
        return pitchDelta / .15 / b0;
    }

    private static double getSensitivityFromPitchGCD(double gcd) {
        double stepOne = pitchToF3(gcd) / 8;
        double stepTwo = Math.cbrt(stepOne);
        double stepThree = stepTwo - .2f;
        return stepThree / .6f;
    }

    private static double getSensitivityFromYawGCD(double gcd) {
        double stepOne = yawToF2(gcd) / 8;
        double stepTwo = Math.cbrt(stepOne);
        double stepThree = stepTwo - .2f;
        return stepThree / .6f;
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        final HeadRotation from = rotationUpdate.getFrom();
        final HeadRotation to = rotationUpdate.getTo();

        final float deltaYaw = Math.abs(to.getYaw() - from.getYaw());
        final float deltaPitch = Math.abs(to.getPitch() - from.getPitch());

        final double gcdYaw = GrimMath.getGcd((long) (deltaYaw * GrimMath.EXPANDER), (long) (lastDeltaYaw * GrimMath.EXPANDER));
        final double gcdPitch = GrimMath.getGcd((long) (deltaPitch * GrimMath.EXPANDER), (long) (lastDeltaPitch * GrimMath.EXPANDER));

        final double dividedYawGcd = gcdYaw / GrimMath.EXPANDER;
        final double dividedPitchGcd = gcdPitch / GrimMath.EXPANDER;

        if (gcdYaw > 90000 && gcdYaw < 2E7 && dividedYawGcd > 0.01f && deltaYaw < 8) {
            yawSamples.add(dividedYawGcd);
        }

        if (gcdPitch > 90000 && gcdPitch < 2E7 && deltaPitch < 8) {
            pitchSamples.add(dividedPitchGcd);
        }

        double modeYaw = 0.0;
        double modePitch = 0.0;

        if (pitchSamples.size() > 5 && yawSamples.size() > 5) {
            modeYaw = yawSamples.getMode();
            modePitch = pitchSamples.getMode();
        }

        final double deltaX = deltaYaw / modeYaw;
        final double deltaY = deltaPitch / modePitch;

        final double sensitivityX = getSensitivityFromYawGCD(modeYaw);
        final double sensitivityY = getSensitivityFromPitchGCD(modePitch);

        rotationUpdate.setSensitivityX(sensitivityX);
        rotationUpdate.setSensitivityY(sensitivityY);

        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.sensitivityX = sensitivityX;
        this.sensitivityY = sensitivityY;
        this.lastDeltaYaw = deltaYaw;
        this.lastDeltaPitch = deltaPitch;
    }
}