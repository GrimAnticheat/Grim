package ac.grim.grimac.predictionengine;

public class UncertaintyHandler {
    public double pistonX;
    public double pistonY;
    public double pistonZ;
    public boolean pistonGravityHack = false;

    public UncertaintyHandler() {
        reset();
    }

    public void reset() {
        pistonX = 0;
        pistonY = 0;
        pistonZ = 0;
    }
}
