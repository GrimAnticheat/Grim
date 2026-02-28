package ac.grim.legacyac.prediction;

public final class PredictionResult {
    private final double maxHorizontal;
    private final double minVertical;
    private final double maxVertical;

    public PredictionResult(double maxHorizontal, double minVertical, double maxVertical) {
        this.maxHorizontal = maxHorizontal;
        this.minVertical = minVertical;
        this.maxVertical = maxVertical;
    }

    public double getMaxHorizontal() {
        return maxHorizontal;
    }

    public double getMinVertical() {
        return minVertical;
    }

    public double getMaxVertical() {
        return maxVertical;
    }
}
