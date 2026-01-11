package ac.grim.grimac.utils.worldborder;

import ac.grim.grimac.utils.math.GrimMath;

public class TickBasedMovingBorderExtent implements BorderExtent {

    private final double from;
    private final double to;
    private final long lerpDuration;
    private long lerpProgress;
    private double size;
    private double previousSize;

    public TickBasedMovingBorderExtent(double from, double to, long durationTicks) {
        this.from = from;
        this.to = to;
        this.lerpDuration = durationTicks;
        this.lerpProgress = durationTicks;
        this.size = calculateSize();
        this.previousSize = this.size;
    }

    private double calculateSize() {
        if (lerpDuration <= 0) return to;
        double progress = (double) (lerpDuration - lerpProgress) / (double) lerpDuration;
        return progress < 1.0D ? GrimMath.lerp(progress, from, to) : to;
    }

    @Override
    public double size() {
        return size;
    }

    @Override
    public double getMinX(double centerX, double absoluteMaxSize) {
        return GrimMath.clamp(centerX - previousSize / 2.0, -absoluteMaxSize, absoluteMaxSize);
    }

    @Override
    public double getMaxX(double centerX, double absoluteMaxSize) {
        return GrimMath.clamp(centerX + previousSize / 2.0, -absoluteMaxSize, absoluteMaxSize);
    }

    @Override
    public double getMinZ(double centerZ, double absoluteMaxSize) {
        return GrimMath.clamp(centerZ - previousSize / 2.0, -absoluteMaxSize, absoluteMaxSize);
    }

    @Override
    public double getMaxZ(double centerZ, double absoluteMaxSize) {
        return GrimMath.clamp(centerZ + previousSize / 2.0, -absoluteMaxSize, absoluteMaxSize);
    }

    @Override
    public BorderExtent tick() {
        if (lerpProgress > 0) {
            lerpProgress--;
            previousSize = size;
            size = calculateSize();
        }
        return update();
    }

    @Override
    public BorderExtent update() {
        if (lerpProgress <= 0) {
            return new StaticBorderExtent(to);
        }
        return this;
    }

}
