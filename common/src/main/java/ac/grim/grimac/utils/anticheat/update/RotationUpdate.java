package ac.grim.grimac.utils.anticheat.update;

import org.jetbrains.annotations.Contract;

public record RotationUpdate(float oldYaw, float oldPitch, float newYaw, float newPitch) {
    @Contract(pure = true)
    public float deltaYaw() {
        return newYaw() - oldYaw();
    }

    @Contract(pure = true)
    public float deltaPitch() {
        return newPitch() - oldPitch();
    }

    @Contract(pure = true)
    public float deltaYawABS() {
        return Math.abs(deltaYaw());
    }

    @Contract(pure = true)
    public float deltaPitchABS() {
        return Math.abs(deltaPitch());
    }
}
