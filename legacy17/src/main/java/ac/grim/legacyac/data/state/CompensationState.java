package ac.grim.legacyac.data.state;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Domain state aggregate for latency-compensation-related data.
 * Tracks teleport sync, velocity samples, knockback samples, pending world
 * changes,
 * and the unified MovementStateSnapshot.
 */
public final class CompensationState {
    // ── Teleport sync ──
    private boolean teleportSyncPending;
    private double pendingTeleportX;
    private double pendingTeleportY;
    private double pendingTeleportZ;
    private long lastTeleportAt;
    private long lastTeleportOrPearlAt;
    private boolean movementUnconfirmed;

    // ── Velocity ──
    private long lastVelocityAt;
    private double lastVelocityXZ;
    private int velocityTicksRemaining;
    private double expectedVelocityXZ;
    private double expectedVelocityY;
    private double expectedVelX;
    private double expectedVelZ;
    private double observedVelocityXZ;
    private double observedVelocityY;

    // ── Pending world changes ──
    private final LinkedList<PendingWorldChange> pendingWorldChanges = new LinkedList<PendingWorldChange>();
    private final MovementStateSnapshot movementStateSnapshot = new MovementStateSnapshot();

    // Slot switch grace
    private long lastSlotSwitchAt;
    private int slotSwitchGraceTicksRemaining;

    // ── Teleport sync methods ──────────────────────────────────────────

    public void beginTeleportSync(double x, double y, double z) {
        teleportSyncPending = true;
        pendingTeleportX = x;
        pendingTeleportY = y;
        pendingTeleportZ = z;
        movementUnconfirmed = true;
        lastTeleportAt = System.currentTimeMillis();
        lastTeleportOrPearlAt = lastTeleportAt;
        enqueuePendingWorldChange(PendingWorldChangeType.TELEPORT, "server-position-sync", 80L);
    }

    public void tryConfirmTeleportSync(double x, double y, double z, boolean hasRecentTxAck) {
        if (!teleportSyncPending)
            return;
        double dx = Math.abs(x - pendingTeleportX);
        double dy = Math.abs(y - pendingTeleportY);
        double dz = Math.abs(z - pendingTeleportZ);
        if (dx > 0.03125D || dy > 0.03125D || dz > 0.03125D)
            return;
        if (hasRecentTxAck) {
            teleportSyncPending = false;
        }
    }

    public boolean isTeleportSyncPending() {
        if (teleportSyncPending) {
            long elapsed = System.currentTimeMillis() - lastTeleportAt;
            if (elapsed > 1500L) {
                teleportSyncPending = false;
                movementUnconfirmed = false;
            }
        }
        return teleportSyncPending;
    }

    public void setLastTeleportAt(long millis) {
        this.lastTeleportAt = millis;
        this.lastTeleportOrPearlAt = millis;
    }

    public void setLastTeleportOrPearlAt(long millis) {
        this.lastTeleportOrPearlAt = millis;
    }

    // ── Velocity methods ───────────────────────────────────────────────

    public void armVelocityWindow(double vx, double vz, double vy, int ticks) {
        expectedVelX = vx;
        expectedVelZ = vz;
        expectedVelocityXZ = Math.sqrt(vx * vx + vz * vz);
        expectedVelocityY = Math.abs(vy);
        observedVelocityXZ = 0.0D;
        observedVelocityY = 0.0D;
        velocityTicksRemaining = ticks;
    }

    public void tickVelocityWindow(double deltaXZ, double deltaY) {
        if (velocityTicksRemaining <= 0)
            return;
        if (deltaXZ > observedVelocityXZ)
            observedVelocityXZ = deltaXZ;
        double absY = Math.abs(deltaY);
        if (absY > observedVelocityY)
            observedVelocityY = absY;
        velocityTicksRemaining--;
    }

    public void clearVelocityWindow() {
        velocityTicksRemaining = 0;
        expectedVelocityXZ = 0.0D;
        expectedVelocityY = 0.0D;
        expectedVelX = 0.0D;
        expectedVelZ = 0.0D;
        observedVelocityXZ = 0.0D;
        observedVelocityY = 0.0D;
    }

    public void setLastVelocityAt(long millis) {
        this.lastVelocityAt = millis;
    }

    public void setLastVelocityXZ(double val) {
        this.lastVelocityXZ = val;
    }

    public void recordPendingVelocityChange(long oneWayDelayMs) {
        enqueuePendingWorldChange(PendingWorldChangeType.VELOCITY, "entity-velocity", oneWayDelayMs);
    }

    public void recordPendingBlockChange(String reason, long oneWayDelayMs) {
        enqueuePendingWorldChange(PendingWorldChangeType.BLOCK_CHANGE, reason, oneWayDelayMs);
    }

    // ── Slot switch ────────────────────────────────────────────────────

    public void markSlotSwitch() {
        lastSlotSwitchAt = System.currentTimeMillis();
    }

    public void startSlotSwitchGrace(int ticks) {
        if (ticks > slotSwitchGraceTicksRemaining) {
            slotSwitchGraceTicksRemaining = ticks;
        }
    }

    public void tickSlotSwitchGrace() {
        if (slotSwitchGraceTicksRemaining > 0)
            slotSwitchGraceTicksRemaining--;
    }

    public boolean isInSlotSwitchGrace() {
        return slotSwitchGraceTicksRemaining > 0;
    }

    public long getLastSlotSwitchAt() {
        return lastSlotSwitchAt;
    }

    // ── Pending world changes ──────────────────────────────────────────

    private void enqueuePendingWorldChange(PendingWorldChangeType type, String reason, long oneWayDelayMs) {
        long now = System.currentTimeMillis();
        long effectiveAt = now + oneWayDelayMs;
        pendingWorldChanges.add(new PendingWorldChange(type, now, effectiveAt, reason));
        while (pendingWorldChanges.size() > 32) {
            pendingWorldChanges.removeFirst();
        }
        movementStateSnapshot.updateFrom(this);
    }

    public void applyPendingWorldChanges() {
        long now = System.currentTimeMillis();
        Iterator<PendingWorldChange> iterator = pendingWorldChanges.iterator();
        while (iterator.hasNext()) {
            PendingWorldChange change = iterator.next();
            if (change.getEffectiveAtMillis() > now)
                continue;
            if (change.getType() == PendingWorldChangeType.TELEPORT) {
                movementUnconfirmed = false;
            }
            iterator.remove();
        }
    }

    public int getPendingWorldChangesCount() {
        applyPendingWorldChanges();
        return pendingWorldChanges.size();
    }

    public List<String> getPendingWorldChangeDebugSnapshot() {
        applyPendingWorldChanges();
        List<String> snapshot = new ArrayList<String>();
        for (PendingWorldChange change : pendingWorldChanges) {
            snapshot.add(change.getType().name() + "@" + change.getEffectiveAtMillis() + ":" + change.getReason());
        }
        return snapshot;
    }

    int countPendingChanges(PendingWorldChangeType type, long nowMillis) {
        int count = 0;
        for (PendingWorldChange change : pendingWorldChanges) {
            if (change.getType() == type && change.getEffectiveAtMillis() > nowMillis) {
                count++;
            }
        }
        return count;
    }

    public MovementStateSnapshot getMovementStateSnapshot() {
        movementStateSnapshot.updateFrom(this);
        return movementStateSnapshot;
    }

    // ── Read interface ──────────────────────────────────────────────────

    public long getLastTeleportAt() {
        return lastTeleportAt;
    }

    public long getLastTeleportOrPearlAt() {
        return lastTeleportOrPearlAt;
    }

    public long getLastVelocityAt() {
        return lastVelocityAt;
    }

    public double getLastVelocityXZ() {
        return lastVelocityXZ;
    }

    public boolean hasPendingVelocityWindow() {
        return velocityTicksRemaining > 0;
    }

    public boolean hasCompletedVelocityWindow() {
        return velocityTicksRemaining <= 0 && (expectedVelocityXZ > 0.0D || expectedVelocityY > 0.0D);
    }

    public double getExpectedVelocityXZ() {
        return expectedVelocityXZ;
    }

    public double getExpectedVelocityY() {
        return expectedVelocityY;
    }

    public double getExpectedVelX() {
        return expectedVelX;
    }

    public double getExpectedVelZ() {
        return expectedVelZ;
    }

    public double getObservedVelocityXZ() {
        return observedVelocityXZ;
    }

    public double getObservedVelocityY() {
        return observedVelocityY;
    }

    public boolean isMovementUnconfirmed() {
        return movementUnconfirmed;
    }

    public void setMovementUnconfirmed(boolean val) {
        this.movementUnconfirmed = val;
    }

    // ── Inner types ─────────────────────────────────────────────────────

    public static final class MovementStateSnapshot {
        private boolean teleportAligned;
        private boolean velocityAligned;
        private boolean blockAligned;
        private int pendingChanges;

        void updateFrom(CompensationState state) {
            state.applyPendingWorldChanges();
            long now = System.currentTimeMillis();
            int pendingTeleport = state.countPendingChanges(PendingWorldChangeType.TELEPORT, now);
            int pendingVelocity = state.countPendingChanges(PendingWorldChangeType.VELOCITY, now);
            int pendingBlock = state.countPendingChanges(PendingWorldChangeType.BLOCK_CHANGE, now);
            this.teleportAligned = pendingTeleport == 0 && !state.teleportSyncPending;
            this.velocityAligned = pendingVelocity == 0;
            this.blockAligned = pendingBlock == 0;
            this.pendingChanges = pendingTeleport + pendingVelocity + pendingBlock;
        }

        public boolean isTeleportAligned() {
            return teleportAligned;
        }

        public boolean isVelocityAligned() {
            return velocityAligned;
        }

        public boolean isBlockAligned() {
            return blockAligned;
        }

        public boolean isFullyAligned() {
            return teleportAligned && velocityAligned && blockAligned;
        }

        public int getPendingChanges() {
            return pendingChanges;
        }
    }

    enum PendingWorldChangeType {
        TELEPORT, VELOCITY, BLOCK_CHANGE
    }

    static final class PendingWorldChange {
        private final PendingWorldChangeType type;
        private final long createdAtMillis;
        private final long effectiveAtMillis;
        private final String reason;

        PendingWorldChange(PendingWorldChangeType type, long createdAtMillis, long effectiveAtMillis, String reason) {
            this.type = type;
            this.createdAtMillis = createdAtMillis;
            this.effectiveAtMillis = effectiveAtMillis;
            this.reason = reason;
        }

        PendingWorldChangeType getType() {
            return type;
        }

        long getCreatedAtMillis() {
            return createdAtMillis;
        }

        long getEffectiveAtMillis() {
            return effectiveAtMillis;
        }

        String getReason() {
            return reason;
        }
    }
}
