package ac.grim.grimac.utils.math;

import java.util.function.Consumer;

public class VanillaMath {
    private static final float[] SIN = make(new float[65536], arrf -> {
        for (int i = 0; i < arrf.length; ++i) {
            arrf[i] = (float) Math.sin((double) i * 3.141592653589793 * 2.0 / 65536.0);
        }
    });

    public static float sin(float f) {
        return SIN[(int) (f * 10430.378f) & 0xFFFF];
    }

    public static float cos(float f) {
        return SIN[(int) (f * 10430.378f + 16384.0f) & 0xFFFF];
    }

    public static int floor(double d) {
        int n = (int) d;
        return d < (double) n ? n - 1 : n;
    }

    public static int ceil(double d) {
        int n = (int) d;
        return d > (double) n ? n + 1 : n;
    }

    public static double clamp(double d, double d2, double d3) {
        if (d < d2) {
            return d2;
        }
        return Math.min(d, d3);
    }

    public static boolean equal(double d, double d2) {
        return Math.abs(d2 - d) < 9.999999747378752E-6;
    }


    // Ripped out of Util line 307
    public static <T> T make(T t, Consumer<T> consumer) {
        consumer.accept(t);
        return t;
    }
}
