package ac.grim.legacyac.data.state;

import org.bukkit.Location;

/**
 * Domain state aggregate for movement-related data.
 * Tracks position deltas, ground/air ticks, yaw/pitch, sprint state, etc.
 *
 * <p>
 * Design: read-interface + bounded write methods only.
 * Checks should NOT modify fields directly; they consume snapshots.
 * </p>
 */
public final class MovementState {
    private int airTicks;
    private int groundTicks;
    private double lastDeltaXZ;
    private double lastDeltaY;
    private double prevDeltaXZ;
    private double prevPrevDeltaXZ;
    private double prevPrevPrevDeltaXZ;
    private double prevDeltaY;
    private float lastYaw;
    private float lastPitch;
    private float lastYawDelta;
    private float lastPitchDelta;
    private float prevYaw;
    private float prevPitch;
    private boolean prevSprinting;
    private boolean previousOnGround;
    private boolean currentOnGround;
    private Location lastSafeLocation;

    // Frame-level position tracking
    private boolean movementFrameInitialized;
    private double lastFrameX;
    private double lastFrameY;
    private double lastFrameZ;
    private float lastFrameYaw;
    private float lastFramePitch;
    private long lastMovementFrameAtNanos;

    // Move window (timer-related)
    private int moveWindow;
    private long moveWindowStart;

    // ── Update methods ──────────────────────────────────────────────────

    /**
     * Called once per movement packet/event to update all movement fields.
     */
    public void onMove(Location from, Location to, boolean onGround, boolean sprinting, boolean sneaking) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        prevPrevPrevDeltaXZ = prevPrevDeltaXZ;
        prevPrevDeltaXZ = prevDeltaXZ;
        prevDeltaXZ = lastDeltaXZ;
        prevDeltaY = lastDeltaY;
        lastDeltaXZ = Math.sqrt(dx * dx + dz * dz);
        lastDeltaY = to.getY() - from.getY();
        prevSprinting = sprinting;

        float yawDelta = Math.abs(to.getYaw() - lastYaw);
        if (yawDelta > 180.0F) {
            yawDelta = 360.0F - yawDelta;
        }
        lastYawDelta = yawDelta;
        lastPitchDelta = Math.abs(to.getPitch() - lastPitch);
        prevYaw = lastYaw;
        prevPitch = lastPitch;
        lastYaw = to.getYaw();
        lastPitch = to.getPitch();

        previousOnGround = currentOnGround;
        currentOnGround = onGround;

        if (onGround) {
            groundTicks++;
            airTicks = 0;
        } else {
            airTicks++;
            groundTicks = 0;
        }

        long now = System.currentTimeMillis();
        if (moveWindowStart == 0L || now - moveWindowStart > 1000L) {
            moveWindowStart = now;
            moveWindow = 0;
        }
        moveWindow++;
    }

    public void setMovementFrame(double x, double y, double z, float yaw, float pitch, long timestampNanos) {
        this.movementFrameInitialized = true;
        this.lastFrameX = x;
        this.lastFrameY = y;
        this.lastFrameZ = z;
        this.lastFrameYaw = yaw;
        this.lastFramePitch = pitch;
        this.lastMovementFrameAtNanos = timestampNanos;
    }

    public void updateSafeLocation(Location location) {
        this.lastSafeLocation = location;
    }

    // ── Read interface ──────────────────────────────────────────────────

    public int getAirTicks() {
        return airTicks;
    }

    public int getGroundTicks() {
        return groundTicks;
    }

    public double getLastDeltaXZ() {
        return lastDeltaXZ;
    }

    public double getLastDeltaY() {
        return lastDeltaY;
    }

    public double getPrevDeltaXZ() {
        return prevDeltaXZ;
    }

    public double getPrevPrevDeltaXZ() {
        return prevPrevDeltaXZ;
    }

    public double getPrevPrevPrevDeltaXZ() {
        return prevPrevPrevDeltaXZ;
    }

    public double getPrevDeltaY() {
        return prevDeltaY;
    }

    public float getLastYaw() {
        return lastYaw;
    }

    public float getLastPitch() {
        return lastPitch;
    }

    public float getLastYawDelta() {
        return lastYawDelta;
    }

    public float getLastPitchDelta() {
        return lastPitchDelta;
    }

    public float getPrevYaw() {
        return prevYaw;
    }

    public float getPrevPitch() {
        return prevPitch;
    }

    public boolean wasSprinting() {
        return prevSprinting;
    }

    public boolean wasOnGround() {
        return previousOnGround;
    }

    public boolean isOnGroundNow() {
        return currentOnGround;
    }

    public Location getLastSafeLocation() {
        return lastSafeLocation;
    }

    public boolean isMovementFrameInitialized() {
        return movementFrameInitialized;
    }

    public double getLastFrameX() {
        return lastFrameX;
    }

    public double getLastFrameY() {
        return lastFrameY;
    }

    public double getLastFrameZ() {
        return lastFrameZ;
    }

    public float getLastFrameYaw() {
        return lastFrameYaw;
    }

    public float getLastFramePitch() {
        return lastFramePitch;
    }

    public long getLastMovementFrameAtNanos() {
        return lastMovementFrameAtNanos;
    }

    public int getMoveWindow() {
        return moveWindow;
    }
}
