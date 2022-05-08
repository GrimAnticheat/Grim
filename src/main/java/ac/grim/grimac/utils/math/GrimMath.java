package ac.grim.grimac.utils.math;

import ac.grim.grimac.utils.data.Pair;
import com.google.common.collect.Lists;
import lombok.experimental.UtilityClass;

import java.util.*;

@UtilityClass
public class GrimMath {
    public final double EXPANDER = Math.pow(2, 24);

    /**
     * @param data - The set of data you want to find the variance from
     * @return - The variance of the numbers.
     * @See - https://en.wikipedia.org/wiki/Variance
     */
    public double getVariance(final Collection<? extends Number> data) {
        int count = 0;

        double sum = 0.0;
        double variance = 0.0;

        double average;

        // Increase the sum and the count to find the average and the standard deviation
        for (final Number number : data) {
            sum += number.doubleValue();
            ++count;
        }

        average = sum / count;

        // Run the standard deviation formula
        for (final Number number : data) {
            variance += Math.pow(number.doubleValue() - average, 2.0);
        }

        return variance;
    }

    /**
     * @param data - The set of numbers / data you want to find the standard deviation from.
     * @return - The standard deviation using the square root of the variance.
     * @See - https://en.wikipedia.org/wiki/Standard_deviation
     * @See - https://en.wikipedia.org/wiki/Variance
     */
    public double getStandardDeviation(final Collection<? extends Number> data) {
        final double variance = getVariance(data);

        // The standard deviation is the square root of variance. (sqrt(s^2))
        return Math.sqrt(variance);
    }

    /**
     * @param data - The set of numbers / data you want to find the skewness from
     * @return - The skewness running the standard skewness formula.
     * @See - https://en.wikipedia.org/wiki/Skewness
     */
    public double getSkewness(final Collection<? extends Number> data) {
        double sum = 0;
        int count = 0;

        final List<Double> numbers = Lists.newArrayList();

        // Get the sum of all the data and the amount via looping
        for (final Number number : data) {
            sum += number.doubleValue();
            ++count;

            numbers.add(number.doubleValue());
        }

        // Sort the numbers to run the calculations in the next part
        Collections.sort(numbers);

        // Run the formula to get skewness
        final double mean = sum / count;
        final double median = (count % 2 != 0) ? numbers.get(count / 2) : (numbers.get((count - 1) / 2) + numbers.get(count / 2)) / 2;
        final double variance = getVariance(data);

        return 3 * (mean - median) / variance;
    }

    public static double magnitude(final double... points) {
        double sum = 0.0;

        for (final double point : points) {
            sum += point * point;
        }

        return Math.sqrt(sum);
    }

    public static int getDistinct(final Collection<? extends Number> collection) {
        Set<Object> set = new HashSet<>(collection);
        return set.size();
    }

    /**
     * @param - collection The collection of the numbers you want to get the duplicates from
     * @return - The duplicate amount
     */
    public static int getDuplicates(final Collection<? extends Number> collection) {
        return collection.size() - getDistinct(collection);
    }

    /**
     * @param - The collection of numbers you want analyze
     * @return - A pair of the high and low outliers
     * @See - https://en.wikipedia.org/wiki/Outlier
     */
    public Pair<List<Double>, List<Double>> getOutliers(final Collection<? extends Number> collection) {
        final List<Double> values = new ArrayList<>();

        for (final Number number : collection) {
            values.add(number.doubleValue());
        }

        final double q1 = getMedian(values.subList(0, values.size() / 2));
        final double q3 = getMedian(values.subList(values.size() / 2, values.size()));

        final double iqr = Math.abs(q1 - q3);
        final double lowThreshold = q1 - 1.5 * iqr, highThreshold = q3 + 1.5 * iqr;

        final Pair<List<Double>, List<Double>> tuple = new Pair<>(new ArrayList<>(), new ArrayList<>());

        for (final Double value : values) {
            if (value < lowThreshold) {
                tuple.getFirst().add(value);
            } else if (value > highThreshold) {
                tuple.getSecond().add(value);
            }
        }

        return tuple;
    }

    /**
     * @param data - The set of numbers/data you want to get the kurtosis from
     * @return - The kurtosis using the standard kurtosis formula
     * @See - https://en.wikipedia.org/wiki/Kurtosis
     */
    public double getKurtosis(final Collection<? extends Number> data) {
        double sum = 0.0;
        int count = 0;

        for (Number number : data) {
            sum += number.doubleValue();
            ++count;
        }

        if (count < 3.0) {
            return 0.0;
        }

        final double efficiencyFirst = count * (count + 1.0) / ((count - 1.0) * (count - 2.0) * (count - 3.0));
        final double efficiencySecond = 3.0 * Math.pow(count - 1.0, 2.0) / ((count - 2.0) * (count - 3.0));
        final double average = sum / count;

        double variance = 0.0;
        double varianceSquared = 0.0;

        for (final Number number : data) {
            variance += Math.pow(average - number.doubleValue(), 2.0);
            varianceSquared += Math.pow(average - number.doubleValue(), 4.0);
        }

        return efficiencyFirst * (varianceSquared / Math.pow(variance / sum, 2.0)) - efficiencySecond;
    }

    /**
     * @param data - The data you want the median from
     * @return - The middle number of that data
     * @See - https://en.wikipedia.org/wiki/Median
     */
    private double getMedian(final List<Double> data) {
        if (data.size() % 2 == 0) {
            return (data.get(data.size() / 2) + data.get(data.size() / 2 - 1)) / 2;
        } else {
            return data.get(data.size() / 2);
        }
    }

    /**
     * @param current  - The current value
     * @param previous - The previous value
     * @return - The GCD of those two values
     */
    public long getGcd(final long current, final long previous) {
        try {
            try {
                return (previous <= 16384L) ? current : getGcd(previous, current % previous);
            } catch (StackOverflowError ignored2) {
                return 100000000000L;
            }
        } catch (Exception ignored) {
            return 100000000000L;
        }
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

    public static int sign(double p_14206_) {
        if (p_14206_ == 0.0D) {
            return 0;
        } else {
            return p_14206_ > 0.0D ? 1 : -1;
        }
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

    public static boolean isCloseEnoughEquals(double d, double d2) {
        return Math.abs(d2 - d) < 9.999999747378752E-6;
    }

    public static double calculateAverage(List<Integer> marks) {
        long sum = 0;
        for (int mark : marks) {
            sum += mark;
        }
        return marks.isEmpty() ? 0 : 1.0 * sum / marks.size();
    }

    public static double calculateAverageLong(List<Long> marks) {
        long sum = 0;
        for (long mark : marks) {
            sum += mark;
        }
        return marks.isEmpty() ? 0 : 1.0 * sum / marks.size();
    }
}
