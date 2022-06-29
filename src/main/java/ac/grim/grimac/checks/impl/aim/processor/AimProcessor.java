package ac.grim.grimac.checks.impl.aim.processor;

import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.data.LastInstance;
import ac.grim.grimac.utils.lists.EvictingQueue;
import ac.grim.grimac.utils.lists.RunningMode;
import ac.grim.grimac.utils.math.GrimMath;

import java.util.ArrayList;
import java.util.List;


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
    public LastInstance lastCinematic = new LastInstance(player);

    EvictingQueue<Float> xRotQueue = new EvictingQueue<>(10);

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        rotationUpdate.setProcessor(this);

        float deltaXRot = rotationUpdate.getDeltaXRotABS();
        float deltaYRot = rotationUpdate.getDeltaYRotABS();

        // GCD/Sensitivity detection
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

        if (this.xRotMode.size() == 50) {
            double modeX = this.xRotMode.getMode();
            this.sensitivityX = convertToSensitivity(modeX);
        }
        if (this.yRotMode.size() == 50) {
            double modeY = this.yRotMode.getMode();
            this.sensitivityY = convertToSensitivity(modeY);
        }

        // Cinematic detection
        if (deltaXRot > 0) {
            xRotQueue.add(rotationUpdate.getDeltaYRot());
            double stdDevAccelerationX = calculateStdDevAcceleration(xRotQueue);

            if (stdDevAccelerationX < 0.1) {
                lastCinematic.reset();
            }
        }
    }

    // In cinematic, you control the acceleration of the acceleration, not the acceleration
    // There is a target value, and you control this target value.
    // Therefore, you progressively will go towards this target
    double calculateStdDevAcceleration(final List<Float> entry) {
        if (entry.size() < 2) return 0;

        List<Double> secondDerivatives = new ArrayList<>();

        double previousAcceleration = entry.get(1) - entry.get(0);
        for (int i = 1; i < entry.size() - 1; i++) {
            double acceleration = entry.get(i + 1) - entry.get(i);
            double secondDerivative = acceleration - previousAcceleration;

            secondDerivatives.add(secondDerivative);

            previousAcceleration = acceleration;
        }

        return GrimMath.calculateSD(secondDerivatives);
    }

    public static double convertToSensitivity(double var13) {
        double var11 = var13 / 0.15F / 8.0D;
        double var9 = Math.cbrt(var11);
        return (var9 - 0.2f) / 0.6f;
    }
}