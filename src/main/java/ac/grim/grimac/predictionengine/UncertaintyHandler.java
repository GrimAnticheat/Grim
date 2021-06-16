package ac.grim.grimac.predictionengine;

public class UncertaintyHandler {
    public double pistonX;
    public double pistonY;
    public double pistonZ;
    public boolean trustClientOnGroundHack = false;
    public boolean collidingWithBoat = false;
    public boolean collidingWithShulker = false;
    public boolean striderOnGround = false;

    public UncertaintyHandler() {
        reset();
    }

    public void reset() {
        pistonX = 0;
        pistonY = 0;
        pistonZ = 0;
        trustClientOnGroundHack = false;
        collidingWithBoat = false;
        collidingWithShulker = false;
    }
}
