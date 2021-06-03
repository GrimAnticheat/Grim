package ac.grim.grimac.predictionengine;

public class UncertaintyHandler {
    public double fireworksX;
    public double fireworksY;
    public double fireworksZ;

    public UncertaintyHandler() {
        reset();
    }

    public void reset() {
        fireworksX = 0;
        fireworksY = 0;
        fireworksZ = 0;
    }
}
