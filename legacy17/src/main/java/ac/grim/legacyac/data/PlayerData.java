package ac.grim.legacyac.data;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public final class PlayerData {
    private final UUID uuid;
    private final Map<String, Double> violations = new HashMap<String, Double>();
    private final Map<String, Double> buffers = new HashMap<String, Double>();
    private final Map<String, Boolean> punishExecuted = new HashMap<String, Boolean>();
    private int airTicks;
    private int groundTicks;
    private long joinAt;
    private long lastAttackAt;
    private long lastTeleportAt;
    private long lastVelocityAt;
    private int clickWindow;
    private long clickWindowStart;
    private int moveWindow;
    private long moveWindowStart;
    private int placeWindow;
    private long placeWindowStart;
    private int breakWindow;
    private long breakWindowStart;
    private int useWindow;
    private long useWindowStart;
    private boolean inventoryOpen;
    private Location lastSafeLocation;
    private double lastDeltaXZ;
    private double lastDeltaY;
    private float lastYaw;
    private float lastPitch;
    private float lastYawDelta;
    private float lastPitchDelta;
    private int velocityTicksRemaining;
    private double expectedVelocityXZ;
    private double expectedVelocityY;
    private double observedVelocityXZ;
    private double observedVelocityY;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.joinAt = System.currentTimeMillis();
    }

    public UUID getUuid() {
        return uuid;
    }

    public void handleMove(Location from, Location to, boolean onGround) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        lastDeltaXZ = Math.sqrt(dx * dx + dz * dz);
        lastDeltaY = to.getY() - from.getY();

        float yawDelta = Math.abs(to.getYaw() - lastYaw);
        if (yawDelta > 180.0F) {
            yawDelta = 360.0F - yawDelta;
        }
        lastYawDelta = yawDelta;
        lastPitchDelta = Math.abs(to.getPitch() - lastPitch);
        lastYaw = to.getYaw();
        lastPitch = to.getPitch();

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

        if (velocityTicksRemaining > 0) {
            if (lastDeltaXZ > observedVelocityXZ) {
                observedVelocityXZ = lastDeltaXZ;
            }
            double absY = Math.abs(lastDeltaY);
            if (absY > observedVelocityY) {
                observedVelocityY = absY;
            }
            velocityTicksRemaining--;
        }
    }

    public int incrementPlaceWindow() {
        long now = System.currentTimeMillis();
        if (placeWindowStart == 0L || now - placeWindowStart > 1000L) {
            placeWindowStart = now;
            placeWindow = 0;
        }
        placeWindow++;
        return placeWindow;
    }

    public int incrementBreakWindow() {
        long now = System.currentTimeMillis();
        if (breakWindowStart == 0L || now - breakWindowStart > 1000L) {
            breakWindowStart = now;
            breakWindow = 0;
        }
        breakWindow++;
        return breakWindow;
    }

    public int incrementUseWindow() {
        long now = System.currentTimeMillis();
        if (useWindowStart == 0L || now - useWindowStart > 1000L) {
            useWindowStart = now;
            useWindow = 0;
        }
        useWindow++;
        return useWindow;
    }

    public void armVelocityWindow(Vector velocity, int ticks) {
        double vx = velocity.getX();
        double vz = velocity.getZ();
        expectedVelocityXZ = Math.sqrt(vx * vx + vz * vz);
        expectedVelocityY = Math.abs(velocity.getY());
        observedVelocityXZ = 0.0D;
        observedVelocityY = 0.0D;
        velocityTicksRemaining = ticks;
    }

    public boolean hasPendingVelocityWindow() {
        return velocityTicksRemaining > 0;
    }

    public boolean hasCompletedVelocityWindow() {
        return velocityTicksRemaining == 0 && (expectedVelocityXZ > 0.0D || expectedVelocityY > 0.0D);
    }

    public void clearVelocityWindow() {
        velocityTicksRemaining = 0;
        expectedVelocityXZ = 0.0D;
        expectedVelocityY = 0.0D;
        observedVelocityXZ = 0.0D;
        observedVelocityY = 0.0D;
    }

    public double getExpectedVelocityXZ() {
        return expectedVelocityXZ;
    }

    public double getExpectedVelocityY() {
        return expectedVelocityY;
    }

    public double getObservedVelocityXZ() {
        return observedVelocityXZ;
    }

    public double getObservedVelocityY() {
        return observedVelocityY;
    }

    public void decayViolations(double amount) {
        for (Iterator<Map.Entry<String, Double>> iterator = violations.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, Double> entry = iterator.next();
            double next = entry.getValue().doubleValue() - amount;
            if (next <= 0.0D) {
                iterator.remove();
            } else {
                entry.setValue(next);
            }
        }

        for (Iterator<Map.Entry<String, Double>> iterator = buffers.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, Double> entry = iterator.next();
            double next = entry.getValue().doubleValue() - (amount * 0.5D);
            if (next <= 0.0D) {
                iterator.remove();
            } else {
                entry.setValue(next);
            }
        }

        if (clickWindowStart != 0L && System.currentTimeMillis() - clickWindowStart > 1500L) {
            clickWindow = 0;
            clickWindowStart = 0L;
        }
    }

    public double addViolation(String check, double amount) {
        double next = getViolation(check) + amount;
        violations.put(check, next);
        return next;
    }

    public double getViolation(String check) {
        Double value = violations.get(check);
        return value == null ? 0.0D : value.doubleValue();
    }

    public double addBuffer(String check, double amount) {
        double next = getBuffer(check) + amount;
        buffers.put(check, next);
        return next;
    }

    public double getBuffer(String check) {
        Double value = buffers.get(check);
        return value == null ? 0.0D : value.doubleValue();
    }

    public boolean hasExecutedPunish(String check) {
        Boolean value = punishExecuted.get(check);
        return value != null && value.booleanValue();
    }

    public void markPunishExecuted(String check) {
        punishExecuted.put(check, Boolean.TRUE);
    }

    public int getAirTicks() {
        return airTicks;
    }

    public int getGroundTicks() {
        return groundTicks;
    }

    public long getJoinAt() {
        return joinAt;
    }

    public void setJoinAt(long joinAt) {
        this.joinAt = joinAt;
    }

    public long getLastAttackAt() {
        return lastAttackAt;
    }

    public void setLastAttackAt(long lastAttackAt) {
        this.lastAttackAt = lastAttackAt;
    }

    public long getLastTeleportAt() {
        return lastTeleportAt;
    }

    public void setLastTeleportAt(long lastTeleportAt) {
        this.lastTeleportAt = lastTeleportAt;
    }

    public long getLastVelocityAt() {
        return lastVelocityAt;
    }

    public void setLastVelocityAt(long lastVelocityAt) {
        this.lastVelocityAt = lastVelocityAt;
    }

    public int getClickWindow() {
        return clickWindow;
    }

    public void setClickWindow(int clickWindow) {
        this.clickWindow = clickWindow;
    }

    public long getClickWindowStart() {
        return clickWindowStart;
    }

    public void setClickWindowStart(long clickWindowStart) {
        this.clickWindowStart = clickWindowStart;
    }

    public int getMoveWindow() {
        return moveWindow;
    }

    public boolean isInventoryOpen() {
        return inventoryOpen;
    }

    public void setInventoryOpen(boolean inventoryOpen) {
        this.inventoryOpen = inventoryOpen;
    }

    public Location getLastSafeLocation() {
        return lastSafeLocation;
    }

    public void setLastSafeLocation(Location lastSafeLocation) {
        this.lastSafeLocation = lastSafeLocation;
    }

    public double getLastDeltaXZ() {
        return lastDeltaXZ;
    }

    public double getLastDeltaY() {
        return lastDeltaY;
    }

    public float getLastYawDelta() {
        return lastYawDelta;
    }

    public float getLastPitchDelta() {
        return lastPitchDelta;
    }
}
