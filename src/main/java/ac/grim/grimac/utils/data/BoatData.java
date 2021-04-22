package ac.grim.grimac.utils.data;

import ac.grim.grimac.utils.enums.BoatEntityStatus;

public class BoatData {
    public boolean boatUnderwater = false;
    public double lastYd;
    // Stuff affects these coords directly?
    public double midTickX;
    public double midTickY;
    public double midTickZ;
    public float landFriction;
    public BoatEntityStatus status;
    public BoatEntityStatus oldStatus;
    public double waterLevel;
    public double boatVelocity;
    public float deltaRotation;
    public float lastYRot;
    public float yRot;

    public BoatData() {

    }
}
