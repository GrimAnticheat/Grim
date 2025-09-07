package ac.grim.grimac.utils.math;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Contract;

@UtilityClass
public class VanillaMath {
    private static final float[] SIN = new float[65536];

    static {
        for (int i = 0; i < SIN.length; ++i) {
            SIN[i] = (float) StrictMath.sin(i * Math.PI * 2d / 65536d);
        }
    }

    @Contract(pure = true)
    public static float sin(float value) {
        return SIN[(int) (value * 10430.378f) & 0xFFFF];
    }

    @Contract(pure = true)
    public static float cos(float value) {
        return SIN[(int) (value * 10430.378f + 16384.0f) & 0xFFFF];
    }
}
