package ac.grim.grimac.utils.data;

import ac.grim.grimac.utils.data.packetentity.JumpableEntity;
import ac.grim.grimac.utils.enums.BoatEntityStatus;
import com.github.retrooper.packetevents.protocol.teleport.RelativeFlag;
import com.github.retrooper.packetevents.util.Vector3d;

import java.util.ArrayDeque;
import java.util.Deque;
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
    public float nextVehicleHorizontal = 0f;
    public float nextVehicleForward = 0f;
    public float vehicleHorizontal = 0f;
    public float vehicleForward = 0f;
    public boolean lastDummy = false;
    public boolean wasVehicleSwitch = false;
    public float playerPitch = 0f;
    public float playerYaw = 0f;
    public final Deque<IntToObjectPair<JumpableEntity>> pendingJumps = new ArrayDeque<>();
    public final ConcurrentLinkedQueue<Teleport> vehicleTeleports = new ConcurrentLinkedQueue<>();
    // The last vehicle move was a teleport the client reported in its regular tick move, not an extra answer to it
    public boolean teleportInTickMove = false;
    // Only the client driving a vehicle sends vehicle moves, so a recent one says who drives it
    public volatile long lastDriveNanos = 0;
    public volatile int lastDriveVehicle = Integer.MIN_VALUE;
    // Where we're snapping the vehicle to, until the VEHICLE_MOVE for it goes out
    public volatile Vector3d pendingSnap = null;
    // The last vehicle move answered one of our snaps, so the server doesn't need it
    public boolean swallowMove = false;
    public SprintingState camelSprintingState = SprintingState.STOPPED;

    // A server teleport of the vehicle the player drives. On relative axes the position is from where the vehicle was
    public record Teleport(int transaction, Vector3d position, RelativeFlag relative, boolean snap) {
    }
}
