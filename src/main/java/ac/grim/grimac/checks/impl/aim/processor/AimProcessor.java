package ac.grim.grimac.checks.impl.aim.processor;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.lists.RunningMode;
import ac.grim.grimac.utils.math.GrimMath;


public class AimProcessor extends Check implements RotationCheck {

    private static final int SIGNIFICANT_SAMPLES_THRESHOLD = 15;
    private static final int TOTAL_SAMPLES_THRESHOLD = 80;

    public AimProcessor(GrimPlayer playerData) {
        super(playerData);
    }

    RunningMode xRotMode = new RunningMode(TOTAL_SAMPLES_THRESHOLD);
    RunningMode yRotMode = new RunningMode(TOTAL_SAMPLES_THRESHOLD);

    float lastXRot;
    float lastYRot;

    public double sensitivityX;
    public double sensitivityY;

    public double divisorX;
    public double divisorY;

    public double modeX, modeY;

    public double deltaDotsX, deltaDotsY;

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        rotationUpdate.setProcessor(this);


        float deltaXRot = rotationUpdate.getDeltaXRotABS();

        this.divisorX = GrimMath.gcd(deltaXRot, lastXRot);
        if (deltaXRot > 0 && deltaXRot < 5 && divisorX > GrimMath.MINIMUM_DIVISOR) {
            this.xRotMode.add(divisorX);
            this.lastXRot = deltaXRot;
        }


        float deltaYRot = rotationUpdate.getDeltaYRotABS();

        this.divisorY = GrimMath.gcd(deltaYRot, lastYRot);

        if (deltaYRot > 0 && deltaYRot < 5 && divisorY > GrimMath.MINIMUM_DIVISOR) {
            this.yRotMode.add(divisorY);
            this.lastYRot = deltaYRot;
        }

        if (this.xRotMode.size() > SIGNIFICANT_SAMPLES_THRESHOLD) {
            Pair<Double, Integer> modeX = this.xRotMode.getMode();
            if (modeX.getSecond() > SIGNIFICANT_SAMPLES_THRESHOLD) {
                this.modeX = modeX.getFirst();
                this.sensitivityX = convertToSensitivity(this.modeX);
            }
        }
        if (this.yRotMode.size() > SIGNIFICANT_SAMPLES_THRESHOLD) {
            Pair<Double, Integer> modeY = this.yRotMode.getMode();
            if (modeY.getSecond() > SIGNIFICANT_SAMPLES_THRESHOLD) {
                this.modeY = modeY.getFirst();
                this.sensitivityY = convertToSensitivity(this.modeY);
            }
        }

        this.deltaDotsX = deltaXRot / modeX;
        this.deltaDotsY = deltaYRot / modeY;
    }

    public static double convertToSensitivity(double var13) {
        double var11 = var13 / 0.15F / 8.0D;
        double var9 = Math.cbrt(var11);
        return (var9 - 0.2f) / 0.6f;
    }
}