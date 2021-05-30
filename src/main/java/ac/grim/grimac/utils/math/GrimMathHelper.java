package ac.grim.grimac.utils.math;

public class GrimMathHelper {
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
}
