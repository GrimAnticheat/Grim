package ac.grim.grimac.utils.nmsutil;

public final class NetherPortalMovement {
    private static final double MAX_SWEEP_DISTANCE = 64.0D;

    private NetherPortalMovement() {
    }

    public static boolean shouldSweep(double deltaX, double deltaY, double deltaZ) {
        return Double.isFinite(deltaX) && Double.isFinite(deltaY) && Double.isFinite(deltaZ)
                && Math.abs(deltaX) <= MAX_SWEEP_DISTANCE
                && Math.abs(deltaY) <= MAX_SWEEP_DISTANCE
                && Math.abs(deltaZ) <= MAX_SWEEP_DISTANCE;
    }
}
