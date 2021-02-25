package org.abyssmc.reaperac.utils;

import org.bukkit.craftbukkit.libs.org.apache.commons.lang3.math.NumberUtils;

import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

public class Mth {
    public static final float SQRT_OF_TWO = Mth.sqrt(2.0f);
    private static final float[] SIN = make(new float[65536], arrf -> {
        for (int i = 0; i < arrf.length; ++i) {
            arrf[i] = (float) Math.sin((double) i * 3.141592653589793 * 2.0 / 65536.0);
        }
    });
    private static final Random RANDOM = new Random();
    private static final int[] MULTIPLY_DE_BRUIJN_BIT_POSITION = new int[]{0, 1, 28, 2, 29, 14, 24, 3, 30, 22, 20, 15, 25, 17, 4, 8, 31, 27, 13, 23, 21, 19, 16, 7, 26, 12, 18, 6, 11, 5, 10, 9};
    private static final double FRAC_BIAS = Double.longBitsToDouble(4805340802404319232L);
    private static final double[] ASIN_TAB = new double[257];
    private static final double[] COS_TAB = new double[257];

    public static float sin(float f) {
        return SIN[(int) (f * 10430.378f) & 0xFFFF];
    }

    public static float cos(float f) {
        return SIN[(int) (f * 10430.378f + 16384.0f) & 0xFFFF];
    }

    public static float sqrt(float f) {
        return (float) Math.sqrt(f);
    }

    public static float sqrt(double d) {
        return (float) Math.sqrt(d);
    }

    public static int floor(float f) {
        int n = (int) f;
        return f < (float) n ? n - 1 : n;
    }

    public static int fastFloor(double d) {
        return (int) (d + 1024.0) - 1024;
    }

    public static int floor(double d) {
        int n = (int) d;
        return d < (double) n ? n - 1 : n;
    }

    public static long lfloor(double d) {
        long l = (long) d;
        return d < (double) l ? l - 1L : l;
    }

    public static float abs(float f) {
        return Math.abs(f);
    }

    public static int abs(int n) {
        return Math.abs(n);
    }

    public static int ceil(float f) {
        int n = (int) f;
        return f > (float) n ? n + 1 : n;
    }

    public static int ceil(double d) {
        int n = (int) d;
        return d > (double) n ? n + 1 : n;
    }

    public static int clamp(int n, int n2, int n3) {
        if (n < n2) {
            return n2;
        }
        if (n > n3) {
            return n3;
        }
        return n;
    }

    public static long clamp(long l, long l2, long l3) {
        if (l < l2) {
            return l2;
        }
        if (l > l3) {
            return l3;
        }
        return l;
    }

    public static float clamp(float f, float f2, float f3) {
        if (f < f2) {
            return f2;
        }
        if (f > f3) {
            return f3;
        }
        return f;
    }

    public static double clamp(double d, double d2, double d3) {
        if (d < d2) {
            return d2;
        }
        if (d > d3) {
            return d3;
        }
        return d;
    }

    public static double clampedLerp(double d, double d2, double d3) {
        if (d3 < 0.0) {
            return d;
        }
        if (d3 > 1.0) {
            return d2;
        }
        return Mth.lerp(d3, d, d2);
    }

    public static double absMax(double d, double d2) {
        if (d < 0.0) {
            d = -d;
        }
        if (d2 < 0.0) {
            d2 = -d2;
        }
        return d > d2 ? d : d2;
    }

    public static int intFloorDiv(int n, int n2) {
        return Math.floorDiv(n, n2);
    }

    public static int nextInt(Random random, int n, int n2) {
        if (n >= n2) {
            return n;
        }
        return random.nextInt(n2 - n + 1) + n;
    }

    public static float nextFloat(Random random, float f, float f2) {
        if (f >= f2) {
            return f;
        }
        return random.nextFloat() * (f2 - f) + f;
    }

    public static double nextDouble(Random random, double d, double d2) {
        if (d >= d2) {
            return d;
        }
        return random.nextDouble() * (d2 - d) + d;
    }

    public static double average(long[] arrl) {
        long l = 0L;
        for (long l2 : arrl) {
            l += l2;
        }
        return (double) l / (double) arrl.length;
    }

    public static boolean equal(float f, float f2) {
        return Math.abs(f2 - f) < 1.0E-5f;
    }

    public static boolean equal(double d, double d2) {
        return Math.abs(d2 - d) < 9.999999747378752E-6;
    }

    public static int positiveModulo(int n, int n2) {
        return Math.floorMod(n, n2);
    }

    public static float positiveModulo(float f, float f2) {
        return (f % f2 + f2) % f2;
    }

    public static double positiveModulo(double d, double d2) {
        return (d % d2 + d2) % d2;
    }

    public static int wrapDegrees(int n) {
        int n2 = n % 360;
        if (n2 >= 180) {
            n2 -= 360;
        }
        if (n2 < -180) {
            n2 += 360;
        }
        return n2;
    }

    public static float wrapDegrees(float f) {
        float f2 = f % 360.0f;
        if (f2 >= 180.0f) {
            f2 -= 360.0f;
        }
        if (f2 < -180.0f) {
            f2 += 360.0f;
        }
        return f2;
    }

    public static double wrapDegrees(double d) {
        double d2 = d % 360.0;
        if (d2 >= 180.0) {
            d2 -= 360.0;
        }
        if (d2 < -180.0) {
            d2 += 360.0;
        }
        return d2;
    }

    public static float degreesDifference(float f, float f2) {
        return Mth.wrapDegrees(f2 - f);
    }

    public static float degreesDifferenceAbs(float f, float f2) {
        return Mth.abs(Mth.degreesDifference(f, f2));
    }

    public static float rotateIfNecessary(float f, float f2, float f3) {
        float f4 = Mth.degreesDifference(f, f2);
        float f5 = Mth.clamp(f4, -f3, f3);
        return f2 - f5;
    }

    public static float approach(float f, float f2, float f3) {
        f3 = Mth.abs(f3);
        if (f < f2) {
            return Mth.clamp(f + f3, f, f2);
        }
        return Mth.clamp(f - f3, f2, f);
    }

    public static float approachDegrees(float f, float f2, float f3) {
        float f4 = Mth.degreesDifference(f, f2);
        return Mth.approach(f, f + f4, f3);
    }

    public static int getInt(String string, int n) {
        return NumberUtils.toInt((String) string, (int) n);
    }

    public static int smallestEncompassingPowerOfTwo(int n) {
        int n2 = n - 1;
        n2 |= n2 >> 1;
        n2 |= n2 >> 2;
        n2 |= n2 >> 4;
        n2 |= n2 >> 8;
        n2 |= n2 >> 16;
        return n2 + 1;
    }

    public static boolean isPowerOfTwo(int n) {
        return n != 0 && (n & n - 1) == 0;
    }

    public static int ceillog2(int n) {
        n = Mth.isPowerOfTwo(n) ? n : Mth.smallestEncompassingPowerOfTwo(n);
        return MULTIPLY_DE_BRUIJN_BIT_POSITION[(int) ((long) n * 125613361L >> 27) & 0x1F];
    }

    public static int log2(int n) {
        return Mth.ceillog2(n) - (Mth.isPowerOfTwo(n) ? 0 : 1);
    }

    public static int roundUp(int n, int n2) {
        int n3;
        if (n2 == 0) {
            return 0;
        }
        if (n == 0) {
            return n2;
        }
        if (n < 0) {
            n2 *= -1;
        }
        if ((n3 = n % n2) == 0) {
            return n;
        }
        return n + n2 - n3;
    }

    public static int color(float f, float f2, float f3) {
        return Mth.color(Mth.floor(f * 255.0f), Mth.floor(f2 * 255.0f), Mth.floor(f3 * 255.0f));
    }

    public static int color(int n, int n2, int n3) {
        int n4 = n;
        n4 = (n4 << 8) + n2;
        n4 = (n4 << 8) + n3;
        return n4;
    }

    public static float frac(float f) {
        return f - (float) Mth.floor(f);
    }

    public static double frac(double d) {
        return d - (double) Mth.lfloor(d);
    }

    // We probably don't need this, there's an error so I removed it
    /*public static long getSeed(Vec3i vec3i) {
        return Mth.getSeed(vec3i.getX(), vec3i.getY(), vec3i.getZ());
    }*/

    public static long getSeed(int n, int n2, int n3) {
        long l = (long) (n * 3129871) ^ (long) n3 * 116129781L ^ (long) n2;
        l = l * l * 42317861L + l * 11L;
        return l >> 16;
    }

    public static UUID createInsecureUUID(Random random) {
        long l = random.nextLong() & 0xFFFFFFFFFFFF0FFFL | 0x4000L;
        long l2 = random.nextLong() & 0x3FFFFFFFFFFFFFFFL | Long.MIN_VALUE;
        return new UUID(l, l2);
    }

    public static UUID createInsecureUUID() {
        return Mth.createInsecureUUID(RANDOM);
    }

    public static double inverseLerp(double d, double d2, double d3) {
        return (d - d2) / (d3 - d2);
    }

    public static double atan2(double d, double d2) {
        boolean bl;
        boolean bl2;
        boolean bl3;
        double d3;
        double d4 = d2 * d2 + d * d;
        if (Double.isNaN(d4)) {
            return Double.NaN;
        }
        boolean bl4 = bl3 = d < 0.0;
        if (bl3) {
            d = -d;
        }
        boolean bl5 = bl = d2 < 0.0;
        if (bl) {
            d2 = -d2;
        }
        boolean bl6 = bl2 = d > d2;
        if (bl2) {
            d3 = d2;
            d2 = d;
            d = d3;
        }
        d3 = Mth.fastInvSqrt(d4);
        double d5 = FRAC_BIAS + (d *= d3);
        int n = (int) Double.doubleToRawLongBits(d5);
        double d6 = ASIN_TAB[n];
        double d7 = COS_TAB[n];
        double d8 = d5 - FRAC_BIAS;
        double d9 = d * d7 - (d2 *= d3) * d8;
        double d10 = (6.0 + d9 * d9) * d9 * 0.16666666666666666;
        double d11 = d6 + d10;
        if (bl2) {
            d11 = 1.5707963267948966 - d11;
        }
        if (bl) {
            d11 = 3.141592653589793 - d11;
        }
        if (bl3) {
            d11 = -d11;
        }
        return d11;
    }

    public static float fastInvSqrt(float f) {
        float f2 = 0.5f * f;
        int n = Float.floatToIntBits(f);
        n = 1597463007 - (n >> 1);
        f = Float.intBitsToFloat(n);
        f *= 1.5f - f2 * f * f;
        return f;
    }

    public static double fastInvSqrt(double d) {
        double d2 = 0.5 * d;
        long l = Double.doubleToRawLongBits(d);
        l = 6910469410427058090L - (l >> 1);
        d = Double.longBitsToDouble(l);
        d *= 1.5 - d2 * d * d;
        return d;
    }

    public static float fastInvCubeRoot(float f) {
        int n = Float.floatToIntBits(f);
        n = 1419967116 - n / 3;
        float f2 = Float.intBitsToFloat(n);
        f2 = 0.6666667f * f2 + 1.0f / (3.0f * f2 * f2 * f);
        f2 = 0.6666667f * f2 + 1.0f / (3.0f * f2 * f2 * f);
        return f2;
    }

    public static int hsvToRgb(float f, float f2, float f3) {
        float f4;
        float f5;
        float f6;
        int n = (int) (f * 6.0f) % 6;
        float f7 = f * 6.0f - (float) n;
        float f8 = f3 * (1.0f - f2);
        float f9 = f3 * (1.0f - f7 * f2);
        float f10 = f3 * (1.0f - (1.0f - f7) * f2);
        switch (n) {
            case 0: {
                f6 = f3;
                f4 = f10;
                f5 = f8;
                break;
            }
            case 1: {
                f6 = f9;
                f4 = f3;
                f5 = f8;
                break;
            }
            case 2: {
                f6 = f8;
                f4 = f3;
                f5 = f10;
                break;
            }
            case 3: {
                f6 = f8;
                f4 = f9;
                f5 = f3;
                break;
            }
            case 4: {
                f6 = f10;
                f4 = f8;
                f5 = f3;
                break;
            }
            case 5: {
                f6 = f3;
                f4 = f8;
                f5 = f9;
                break;
            }
            default: {
                throw new RuntimeException("Something went wrong when converting from HSV to RGB. Input was " + f + ", " + f2 + ", " + f3);
            }
        }
        int n2 = Mth.clamp((int) (f6 * 255.0f), 0, 255);
        int n3 = Mth.clamp((int) (f4 * 255.0f), 0, 255);
        int n4 = Mth.clamp((int) (f5 * 255.0f), 0, 255);
        return n2 << 16 | n3 << 8 | n4;
    }

    public static int murmurHash3Mixer(int n) {
        n ^= n >>> 16;
        n *= -2048144789;
        n ^= n >>> 13;
        n *= -1028477387;
        n ^= n >>> 16;
        return n;
    }

    public static int binarySearch(int n, int n2, IntPredicate intPredicate) {
        int n3 = n2 - n;
        while (n3 > 0) {
            int n4 = n3 / 2;
            int n5 = n + n4;
            if (intPredicate.test(n5)) {
                n3 = n4;
                continue;
            }
            n = n5 + 1;
            n3 -= n4 + 1;
        }
        return n;
    }

    public static float lerp(float f, float f2, float f3) {
        return f2 + f * (f3 - f2);
    }

    public static double lerp(double d, double d2, double d3) {
        return d2 + d * (d3 - d2);
    }

    public static double lerp2(double d, double d2, double d3, double d4, double d5, double d6) {
        return Mth.lerp(d2, Mth.lerp(d, d3, d4), Mth.lerp(d, d5, d6));
    }

    public static double lerp3(double d, double d2, double d3, double d4, double d5, double d6, double d7, double d8, double d9, double d10, double d11) {
        return Mth.lerp(d3, Mth.lerp2(d, d2, d4, d5, d6, d7), Mth.lerp2(d, d2, d8, d9, d10, d11));
    }

    public static double smoothstep(double d) {
        return d * d * d * (d * (d * 6.0 - 15.0) + 10.0);
    }

    public static int sign(double d) {
        if (d == 0.0) {
            return 0;
        }
        return d > 0.0 ? 1 : -1;
    }

    public static float rotLerp(float f, float f2, float f3) {
        return f2 + f * Mth.wrapDegrees(f3 - f2);
    }

    @Deprecated
    public static float rotlerp(float f, float f2, float f3) {
        float f4;
        for (f4 = f2 - f; f4 < -180.0f; f4 += 360.0f) {
        }
        while (f4 >= 180.0f) {
            f4 -= 360.0f;
        }
        return f + f3 * f4;
    }

    @Deprecated
    public static float rotWrap(double d) {
        while (d >= 180.0) {
            d -= 360.0;
        }
        while (d < -180.0) {
            d += 360.0;
        }
        return (float) d;
    }

    public static float triangleWave(float f, float f2) {
        return (Math.abs(f % f2 - f2 * 0.5f) - f2 * 0.25f) / (f2 * 0.25f);
    }

    public static float square(float f) {
        return f * f;
    }

    static {
        for (int i = 0; i < 257; ++i) {
            double d = (double) i / 256.0;
            double d2 = Math.asin(d);
            Mth.COS_TAB[i] = Math.cos(d2);
            Mth.ASIN_TAB[i] = d2;
        }
    }

    // Ripped out of Util line 307
    public static <T> T make(T t, Consumer<T> consumer) {
        consumer.accept(t);
        return t;
    }
}
