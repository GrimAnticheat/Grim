package ac.grim.grimac.utils.math;

import java.util.List;

public class GrimMath {
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
