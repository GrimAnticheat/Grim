package ac.grim.grimac.utils.data;

import ac.grim.grimac.utils.enums.BoatEntityStatus;
import io.github.retrooper.packetevents.utils.pair.Pair;
import io.github.retrooper.packetevents.utils.vector.Vector3d;

import java.util.concurrent.ConcurrentLinkedQueue;

public class VehicleData {
    public boolean boatUnderwater = false;
    public double lastYd;
    public double midTickY;
    public float landFriction;
    public BoatEntityStatus status;
    public BoatEntityStatus oldStatus;
    public double waterLevel;
    public float deltaRotation;
    public float vehicleHorizontal = 0f;
    public float vehicleForward = 0f;
    public int lastVehicleSwitch = 1000;
    public boolean lastDummy = false;
    public ConcurrentLinkedQueue<Pair<Integer, Vector3d>> vehicleTeleports = new ConcurrentLinkedQueue<>();
    public float horseJump = 0;
    public boolean horseJumping = false;

    public VehicleData() {

    }
}
