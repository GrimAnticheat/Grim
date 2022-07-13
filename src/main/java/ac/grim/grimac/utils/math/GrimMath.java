package ac.grim.grimac.utils.math;

import com.google.common.util.concurrent.AtomicDouble;
import java.util.Collection;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class GrimMath {
    public static final double MINIMUM_DIVISOR = ((Math.pow(0.2f, 3) * 8) * 0.15) - 1e-3; // 1e-3 for float imprecision


    public static double gcd(double a, double b) {
        if (a == 0) return 0;

        // Make sure a is larger than b
        if (a < b) {
            double temp = a;
            a = b;
            b = temp;
        }

        while (a > MINIMUM_DIVISOR) { // Minimum minecraft sensitivity
            double temp = a - (Math.floor(a / b) * b);
            a = b;
            b = temp;
        }

        return a;
    }

    public static double calculateSD(List<Double> numbers) {
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

    public static int floor(double d) {
        return (int) Math.floor(d);
    }

    public static int ceil(double d) {
        return (int) Math.ceil(d);
    }

    public static double clamp(double d, double d2, double d3) {
        if (d < d2) {
            return d2;
        }
        return Math.min(d, d3);
    }

    public static float clampFloat(float d, float d2, float d3) {
        if (d < d2) {
            return d2;
        }
        return Math.min(d, d3);
    }

    public static double lerp(double lerpAmount, double start, double end) {
        return start + lerpAmount * (end - start);
    }

    public static double frac(double p_14186_) {
        return p_14186_ - lfloor(p_14186_);
    }

    public static long lfloor(double p_14135_) {
        long i = (long) p_14135_;
        return p_14135_ < (double) i ? i - 1L : i;
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

    public static boolean betweenRange(double value, double min, double max) {
        return value > min && value < max;
    }

    public static boolean inRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    /**
     * Calculates the average (mean) of {@param values}
     *
     * @author Sim0n (https://github.com/sim0n/Nemesis)
     * @param values The number values
     * @return The average (mean) of {@param values}
     */
    public double getAverage(Collection<? extends Number> values) {
        return values.stream()
                .mapToDouble(Number::doubleValue)
                .average()
                .orElse(0D);
    }

    /**
     * Calculates the standard deviation of {@param values}
     *
     * @author Sim0n (https://github.com/sim0n/Nemesis)
     * @param values The number values
     * @return The standard deviation of {@param values}
     */
    public double getStandardDeviation(Collection<? extends Number> values) {
        double average = getAverage(values);

        AtomicDouble variance = new AtomicDouble(0D);

        values.forEach(delay -> variance.getAndAdd(Math.pow(delay.doubleValue() - average, 2D)));

        return Math.sqrt(variance.get() / values.size());
    }

    /**
     * Calculates the kurtosis of {@param values}
     *
     * @author Sim0n (https://github.com/sim0n/Nemesis)
     * @param values The number values
     * @return The kurtosis of {@param values}
     */
    public double getKurtosis(Collection<? extends Number> values) {
        double n = values.size();

        if (n < 3)
            return Double.NaN;

        double average = getAverage(values);
        double stDev = getStandardDeviation(values);

        AtomicDouble accum = new AtomicDouble(0D);

        values.forEach(delay -> accum.getAndAdd(Math.pow(delay.doubleValue() - average, 4D)));

        return n * (n + 1) / ((n - 1) * (n - 2) * (n - 3)) *
                (accum.get() / Math.pow(stDev, 4D)) - 3 *
                Math.pow(n - 1, 2D) / ((n - 2) * (n - 3));
    }

    /**
     * Gets the cps of {@param values}
     *
     * @author Sim0n (https://github.com/sim0n/Nemesis)
     * @param values The number values
     * @return The cps
     */
    public double getCps(Collection<? extends Number> values) {
        // 1 second = 20 ticks
        return 20 / getAverage(values);
    }
}
