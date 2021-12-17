package ac.grim.grimac.utils.math;

import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class GrimMath {

    public int floor(double d) {
        return (int) Math.floor(d);
    }

    public int ceil(double d) {
        return (int) Math.ceil(d);
    }

    public double clamp(double d, double d2, double d3) {
        if (d < d2) {
            return d2;
        }
        return Math.min(d, d3);
    }

    public float clampFloat(float d, float d2, float d3) {
        if (d < d2) {
            return d2;
        }
        return Math.min(d, d3);
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
    public double distanceToHorizontalCollision(double position) {
        return Math.min(Math.abs(position % (1 / 640d)), Math.abs(Math.abs(position % (1 / 640d)) - (1 / 640d)));
    }

    public boolean equal(double d, double d2) {
        return Math.abs(d2 - d) < 9.999999747378752E-6;
    }

    public double calculateAverage(List<Integer> marks) {
        long sum = 0;
        for (Integer mark : marks) {
            sum += mark;
        }
        return marks.isEmpty() ? 0 : 1.0 * sum / marks.size();
    }

    public double calculateAverageLong(List<Long> marks) {
        long sum = 0;
        for (Long mark : marks) {
            sum += mark;
        }
        return marks.isEmpty() ? 0 : 1.0 * sum / marks.size();
    }
}
