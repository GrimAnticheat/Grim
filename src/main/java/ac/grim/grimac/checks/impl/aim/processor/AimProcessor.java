package ac.grim.grimac.checks.impl.aim.processor;

import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.lists.RunningMode;
import ac.grim.grimac.utils.math.GrimMath;


public class AimProcessor extends RotationCheck {

    public AimProcessor(GrimPlayer playerData) {
        super(playerData);
    }

    RunningMode<Double> xRotMode = new RunningMode<>(50);
    RunningMode<Double> yRotMode = new RunningMode<>(50);

    float lastXRot;
    float lastYRot;

    public double sensitivityX;
    public double sensitivityY;

    public double divisorX;
    public double divisorY;

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        float deltaXRot = rotationUpdate.getDeltaXRotABS();
        float deltaYRot = rotationUpdate.getDeltaYRotABS();

        this.divisorX = GrimMath.gcd(deltaXRot, lastXRot);

        if (deltaXRot > 0 && deltaXRot < 5) {
            if (divisorX > GrimMath.MINIMUM_DIVISOR) {
                this.xRotMode.add(divisorX);
                this.lastXRot = deltaXRot;
            }
        }

        this.divisorY = GrimMath.gcd(deltaYRot, lastYRot);

        if (deltaYRot > 0 && deltaYRot < 5) {
            if (divisorY > GrimMath.MINIMUM_DIVISOR) {
                this.yRotMode.add(divisorY);
                this.lastYRot = deltaYRot;
            }
        }

        double modeX = this.xRotMode.getMode();
        double modeY = this.yRotMode.getMode();

        this.sensitivityX = convertToSensitivity(modeX);
        this.sensitivityY = convertToSensitivity(modeY);
    }

    public static double convertToSensitivity(double var13) {
        double var11 = var13 / 0.15F / 8.0D;
        double var9 = Math.cbrt(var11);
        return (var9 - 0.2f) / 0.6f;
    }
}