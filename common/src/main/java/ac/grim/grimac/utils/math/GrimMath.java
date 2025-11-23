package ac.grim.grimac.utils.math;

import com.github.retrooper.packetevents.util.Vector3i;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

@UtilityClass
public class GrimMath {
    public static final double MINIMUM_DIVISOR = ((Math.pow(0.2f, 3) * 8) * 0.15) - 1e-3; // 1e-3 for float imprecision
    private static final float DEGREES_TO_RADIANS = (float) Math.PI / 180f;

    @Contract(pure = true)
    public static double gcd(double a, double b) {
        if (a == 0) return 0;

        // Make sure a is larger than b
        if (a < b) {
            double temp = a;
            a = b;
            b = temp;
        }

        while (b > MINIMUM_DIVISOR) { // Minimum minecraft sensitivity
            double temp = a - (Math.floor(a / b) * b);
            a = b;
            b = temp;
        }

        return a;
    }

    @Contract(pure = true)
    public static double calculateSD(@NotNull List<@NotNull Double> numbers) {
        double sum = 0.0;
        double standardDeviation = 0.0;

        for (double rotation : numbers) {
            sum += rotation;
        }

        double mean = sum / numbers.size();

        for (double num : numbers) {
            standardDeviation += Math.pow(num - mean, 2);
        }

        return Math.sqrt(standardDeviation / numbers.size());
    }

    @Contract(pure = true)
    public static int floor(double d) {
        return (int) Math.floor(d);
    }

    @Contract(pure = true)
    public static int ceil(double d) {
        return (int) Math.ceil(d);
    }

    // Should produce the same output as Math.floor() and Math.ceil() but mojang do it differently
    // Replicating what they do jussst in case
    @Contract(pure = true)
    public static int mojangFloor(double num) {
        final int floor = (int) num;
        return floor == num ? floor : floor - (int) (Double.doubleToRawLongBits(num) >>> 63);
    }

    @Contract(pure = true)
    public static int mojangCeil(final double num) {
        final int floor = (int) num;
        return floor == num ? floor : floor + (int) (~Double.doubleToRawLongBits(num) >>> 63);
    }

    @Contract(pure = true)
    public static double clamp(double num, double min, double max) {
        if (num < min) {
            return min;
        }
        return Math.min(num, max);
    }

    @Contract(pure = true)
    public static int clamp(int num, int min, int max) {
        if (num < min) {
            return min;
        }
        return Math.min(num, max);
    }

    @Contract(pure = true)
    public static float clamp(float num, float min, float max) {
        if (num < min) {
            return min;
        }
        return Math.min(num, max);
    }

    @Contract(pure = true)
    public static double lerp(double lerpAmount, double start, double end) {
        return start + lerpAmount * (end - start);
    }

    @Contract(pure = true)
    public static double frac(double p_14186_) {
        return p_14186_ - lfloor(p_14186_);
    }

    @Contract(pure = true)
    public static long lfloor(double p_14135_) {
        long i = (long) p_14135_;
        return p_14135_ < (double) i ? i - 1L : i;
    }

    @Contract(pure = true)
    public static int sign(double x) {
        return x == 0.0 ? 0 : x > 0.0 ? 1 : -1;
    }

    @Contract(pure = true)
    public static float square(float value) {
        return value * value;
    }

    @Contract(pure = true)
    public static float sqrt(float value) {
        return (float)Math.sqrt(value);
    }

    // Find the closest distance to (1 / 64)
    // All poses horizontal length is 0.2 or 0.6 (0.1 or 0.3)
    // and we call this from the player's position
    //
    // We must find the minimum of the three numbers:
    // Distance to (1 / 64) when we are around -0.1
    // Distance to (1 / 64) when we are around 0
    // Distance to (1 / 64) when we are around 0.1
    //
    // Someone should likely just refactor this entire method, although it is cold being called twice every movement
    public static double distanceToHorizontalCollision(double position) {
        return Math.min(Math.abs(position % (1 / 640d)), Math.abs(Math.abs(position % (1 / 640d)) - (1 / 640d)));
    }

    @Contract(pure = true)
    public static boolean betweenRange(double value, double min, double max) {
        return value > min && value < max;
    }

    @Contract(pure = true)
    public static boolean inRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    @Contract(pure = true)
    public static boolean inRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    @Contract(pure = true)
    public static boolean isNearlySame(double a, double b, double epoch) {
        return Math.abs(a - b) < epoch;
    }

    @Contract(pure = true)
    public static long hashCode(double x, int y, double z) {
        long l = (long) (x * 3129871) ^ (long) z * 116129781L ^ (long) y;
        l = l * l * 42317861L + l * 11L;
        return l >> 16;
    }

    @Contract(pure = true)
    public static float radians(float degrees) {
        return degrees * DEGREES_TO_RADIANS;
    }

    private static final int[] MULTIPLY_DE_BRUIJN_BIT_POSITION = new int[]{
            0, 1, 28, 2, 29, 14, 24, 3, 30, 22, 20, 15, 25, 17, 4, 8, 31, 27, 13, 23, 21, 19, 16, 7, 26, 12, 18, 6, 11, 5, 10, 9
    };

    public static final int PACKED_HORIZONTAL_LENGTH = 1 + GrimMath.log2(GrimMath.smallestEncompassingPowerOfTwo(30000000));
    public static final int PACKED_Y_LENGTH = 64 - 2 * PACKED_HORIZONTAL_LENGTH;
    private static final long PACKED_X_MASK = (1L << PACKED_HORIZONTAL_LENGTH) - 1L;
    private static final long PACKED_Y_MASK = (1L << PACKED_Y_LENGTH) - 1L;
    private static final long PACKED_Z_MASK = (1L << PACKED_HORIZONTAL_LENGTH) - 1L;
    private static final int Z_OFFSET = PACKED_Y_LENGTH;
    private static final int X_OFFSET = PACKED_Y_LENGTH + PACKED_HORIZONTAL_LENGTH;

    @Contract(pure = true)
    public static long asLong(Vector3i vector) {
        return asLong(vector.getX(), vector.getY(), vector.getZ());
    }

    @Contract(pure = true)
    public static long asLong(int x, int y, int z) {
        return (x & PACKED_X_MASK) << X_OFFSET
                | y & PACKED_Y_MASK
                | (z & PACKED_Z_MASK) << Z_OFFSET;
    }

    public static int log2(int value) {
        return ceillog2(value) - (isPowerOfTwo(value) ? 0 : 1);
    }

    public static int ceillog2(int value) {
        value = isPowerOfTwo(value) ? value : smallestEncompassingPowerOfTwo(value);
        return MULTIPLY_DE_BRUIJN_BIT_POSITION[(int)(value * 125613361L >> 27) & 31];
    }

    @Contract(pure = true)
    public static boolean isPowerOfTwo(int value) {
        return value != 0 && (value & value - 1) == 0;
    }

    @Contract(pure = true)
    public static int smallestEncompassingPowerOfTwo(int value) {
        int output = value - 1;
        output |= output >> 1;
        output |= output >> 2;
        output |= output >> 4;
        output |= output >> 8;
        output |= output >> 16;
        return output + 1;
    }

    @Contract(pure = true)
    public static boolean equal(double first, double second) {
        return Math.abs(second - first) < 1.0E-5F;
    }

    @Contract(pure = true)
    public static double square(double num) {
        return num * num;
    }

    // ========== for autoclicker checks ==========

    public static double getCps(final Collection<? extends Number> data) {
        return 20.0 / getAverage(data) * 50.0;
    }

    public static double getAverageLong(final Collection<Long> data) {
        if (data == null || data.isEmpty()) {
            return 0.0;
        }
        long sum = 0L;
        for (final Long number : data) {
            sum += number;
        }
        return (double) sum / data.size();
    }

    public static double getAverageInt(final Collection<Integer> data) {
        if (data == null || data.isEmpty()) {
            return 0.0;
        }
        int sum = 0;
        for (final Integer number : data) {
            sum += number;
        }
        return (double) sum / data.size();
    }

    public static double getStandardDeviationLong(final Collection<Long> data) {
        final double variance = getVarianceLong(data);
        return Math.sqrt(variance);
    }

    public static double getVarianceLong(final Collection<Long> data) {
        int count = 0;
        double sum = 0.0;
        double variance = 0.0;
        for (final Long number : data) {
            sum += number;
            ++count;
        }
        final double average = sum / count;
        for (final Long number : data) {
            variance += Math.pow(number - average, 2.0);
        }
        return variance;
    }

    public static double getStandardDeviationInt(final Collection<Integer> data) {
        final double variance = getVarianceInt(data);
        return Math.sqrt(variance);
    }

    public static double getVarianceInt(final Collection<Integer> data) {
        int count = 0;
        double sum = 0.0;
        double variance = 0.0;
        for (final Integer number : data) {
            sum += number;
            ++count;
        }
        final double average = sum / count;
        for (final Integer number : data) {
            variance += Math.pow(number - average, 2.0);
        }
        return variance;
    }

    // also necesary for autoclicker checks
    public static double getAverage(final Collection<? extends Number> data) {
        if (data == null || data.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (final Number number : data) {
            sum += number.doubleValue();
        }
        return sum / data.size();
    }

    public static double getStandardDeviation(final Collection<? extends Number> data) {
        final double variance = getVariance(data);
        return Math.sqrt(variance);
    }

    public static double getVariance(final Collection<? extends Number> data) {
        int count = 0;
        double sum = 0.0;
        double variance = 0.0;
        for (final Number number : data) {
            sum += number.doubleValue();
            ++count;
        }
        final double average = sum / count;
        for (final Number number : data) {
            variance += Math.pow(number.doubleValue() - average, 2.0);
        }
        return variance;
    }

    public static double
    getPlayerSpeed(GrimPlayer player) {
        double deltaX = Math.abs(player.x - player.lastX);
        double deltaY = Math.abs(player.y - player.lastY);
        double deltaZ = Math.abs(player.z - player.lastZ);
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        double speed = distance / 0.05D;

            speed -= (double) player.fallDistance / 0.05D;
        return speed;
    }

    public static int getDistinct(Collection<? extends Number> data) {
        return (int)data.stream().distinct().count();
    }
}
