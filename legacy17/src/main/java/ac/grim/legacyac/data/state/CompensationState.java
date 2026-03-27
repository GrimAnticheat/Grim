package ac.grim.legacyac.data.state;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Domain state aggregate for latency-compensation-related data.
 * Tracks teleport sync, velocity windows, and transaction-anchored world updates.
 */
public final class CompensationState {
    private static final long TELEPORT_SYNC_TIMEOUT_MS = 1500L;
    private static final long PENDING_CHANGE_TIMEOUT_MS = 2000L;

    // Teleport sync
    private boolean teleportSyncPending;
    private boolean teleportPositionConfirmed;
    private double pendingTeleportX;
    private double pendingTeleportY;
    private double pendingTeleportZ;
    private long lastTeleportAt;
    private long lastTeleportOrPearlAt;
    private boolean movementUnconfirmed;

    // Velocity
    private long lastVelocityAt;
    private double lastVelocityXZ;
    private int velocityTicksRemaining;
    private double expectedVelocityXZ;
    private double expectedVelocityY;
    private double expectedVelX;
    private double expectedVelZ;
    private double observedVelocityXZ;
    private double observedVelocityY;

    // Pending world changes
    private final LinkedList<PendingWorldChange> pendingWorldChanges = new LinkedList<PendingWorldChange>();
    private final MovementStateSnapshot movementStateSnapshot = new MovementStateSnapshot();

    // Slot switch grace
    private long lastSlotSwitchAt;
    private int slotSwitchGraceTicksRemaining;

    public void beginTeleportSync(double x, double y, double z) {
        beginTeleportSync(x, y, z, (short) 0);
    }

    public void beginTeleportSync(double x, double y, double z, short anchorTransactionId) {
        teleportSyncPending = true;
        teleportPositionConfirmed = false;
        pendingTeleportX = x;
        pendingTeleportY = y;
        pendingTeleportZ = z;
        movementUnconfirmed = true;
        lastTeleportAt = System.currentTimeMillis();
        lastTeleportOrPearlAt = lastTeleportAt;
        enqueuePendingWorldChange(PendingWorldChangeType.TELEPORT, "server-position-sync", anchorTransactionId);
    }

    public void tryConfirmTeleportSync(double x, double y, double z, boolean hasRecentTxAck) {
        tryConfirmTeleportSync(x, y, z);
    }

    public void tryConfirmTeleportSync(double x, double y, double z) {
        if (!teleportSyncPending) {
            return;
        }
        double dx = Math.abs(x - pendingTeleportX);
        double dy = Math.abs(y - pendingTeleportY);
        double dz = Math.abs(z - pendingTeleportZ);
        if (dx > 0.03125D || dy > 0.03125D || dz > 0.03125D) {
            return;
        }
        teleportPositionConfirmed = true;
        completeTeleportSyncIfReady();
    }

    public boolean isTeleportSyncPending() {
        expirePendingWorldChanges();
        if (teleportSyncPending) {
            long elapsed = System.currentTimeMillis() - lastTeleportAt;
            if (elapsed > TELEPORT_SYNC_TIMEOUT_MS) {
                teleportSyncPending = false;
                teleportPositionConfirmed = false;
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
        if (velocityTicksRemaining <= 0) {
            return;
        }
        if (deltaXZ > observedVelocityXZ) {
            observedVelocityXZ = deltaXZ;
        }
        double absY = Math.abs(deltaY);
        if (absY > observedVelocityY) {
            observedVelocityY = absY;
        }
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
        recordPendingVelocityChange((short) 0);
    }

    public void recordPendingVelocityChange(short anchorTransactionId) {
        enqueuePendingWorldChange(PendingWorldChangeType.VELOCITY, "entity-velocity", anchorTransactionId);
    }

    public void recordPendingBlockChange(String reason, long oneWayDelayMs) {
        recordPendingBlockChange(reason, (short) 0);
    }

    public void recordPendingBlockChange(String reason, short anchorTransactionId) {
        enqueuePendingWorldChange(PendingWorldChangeType.BLOCK_CHANGE, reason, anchorTransactionId);
    }

    public void acknowledgeTransaction(short actionId) {
        Iterator<PendingWorldChange> iterator = pendingWorldChanges.iterator();
        while (iterator.hasNext()) {
            PendingWorldChange change = iterator.next();
            if (change.getAnchorTransactionId() != actionId) {
                continue;
            }
            if (change.getType() == PendingWorldChangeType.TELEPORT) {
                iterator.remove();
                completeTeleportSyncIfReady();
                continue;
            }
            iterator.remove();
        }
        movementStateSnapshot.updateFrom(this);
    }

    public void markSlotSwitch() {
        lastSlotSwitchAt = System.currentTimeMillis();
    }

    public void startSlotSwitchGrace(int ticks) {
        if (ticks > slotSwitchGraceTicksRemaining) {
            slotSwitchGraceTicksRemaining = ticks;
        }
    }

    public void tickSlotSwitchGrace() {
        if (slotSwitchGraceTicksRemaining > 0) {
            slotSwitchGraceTicksRemaining--;
        }
    }

    public boolean isInSlotSwitchGrace() {
        return slotSwitchGraceTicksRemaining > 0;
    }

    public long getLastSlotSwitchAt() {
        return lastSlotSwitchAt;
    }

    private void enqueuePendingWorldChange(PendingWorldChangeType type, String reason, short anchorTransactionId) {
        long now = System.currentTimeMillis();
        pendingWorldChanges.add(new PendingWorldChange(type, now, now + PENDING_CHANGE_TIMEOUT_MS, anchorTransactionId, reason));
        while (pendingWorldChanges.size() > 32) {
            pendingWorldChanges.removeFirst();
        }
        movementStateSnapshot.updateFrom(this);
    }

    public void applyPendingWorldChanges() {
        expirePendingWorldChanges();
    }

    public int getPendingWorldChangesCount() {
        expirePendingWorldChanges();
        return pendingWorldChanges.size();
    }

    public List<String> getPendingWorldChangeDebugSnapshot() {
        expirePendingWorldChanges();
        List<String> snapshot = new ArrayList<String>();
        for (PendingWorldChange change : pendingWorldChanges) {
            snapshot.add(change.getType().name() + "#"
                    + change.getAnchorTransactionId() + ":" + change.getReason());
        }
        return snapshot;
    }

    int countPendingChanges(PendingWorldChangeType type) {
        int count = 0;
        for (PendingWorldChange change : pendingWorldChanges) {
            if (change.getType() == type) {
                count++;
            }
        }
        return count;
    }

    public MovementStateSnapshot getMovementStateSnapshot() {
        movementStateSnapshot.updateFrom(this);
        return movementStateSnapshot;
    }

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

    private void expirePendingWorldChanges() {
        long now = System.currentTimeMillis();
        Iterator<PendingWorldChange> iterator = pendingWorldChanges.iterator();
        while (iterator.hasNext()) {
            PendingWorldChange change = iterator.next();
            if (change.getExpiresAtMillis() > now) {
                continue;
            }
            if (change.getType() == PendingWorldChangeType.TELEPORT) {
                teleportSyncPending = false;
                teleportPositionConfirmed = false;
                movementUnconfirmed = false;
            }
            iterator.remove();
        }
    }

    private void completeTeleportSyncIfReady() {
        if (!teleportSyncPending) {
            return;
        }
        if (!teleportPositionConfirmed) {
            return;
        }
        if (countPendingChanges(PendingWorldChangeType.TELEPORT) > 0) {
            return;
        }
        teleportSyncPending = false;
        teleportPositionConfirmed = false;
        movementUnconfirmed = false;
    }

    public static final class MovementStateSnapshot {
        private boolean teleportAligned;
        private boolean velocityAligned;
        private boolean blockAligned;
        private int pendingChanges;
        private AlignmentBlocker primaryBlocker = AlignmentBlocker.NONE;
        private boolean enforceable;

        void updateFrom(CompensationState state) {
            state.expirePendingWorldChanges();
            int pendingTeleport = state.countPendingChanges(PendingWorldChangeType.TELEPORT);
            int pendingVelocity = state.countPendingChanges(PendingWorldChangeType.VELOCITY);
            int pendingBlock = state.countPendingChanges(PendingWorldChangeType.BLOCK_CHANGE);
            this.teleportAligned = pendingTeleport == 0 && !state.teleportSyncPending;
            this.velocityAligned = pendingVelocity == 0;
            this.blockAligned = pendingBlock == 0;
            this.pendingChanges = pendingTeleport + pendingVelocity + pendingBlock;
            if (!teleportAligned) {
                primaryBlocker = AlignmentBlocker.TELEPORT;
            } else if (!blockAligned) {
                primaryBlocker = AlignmentBlocker.BLOCK;
            } else if (!velocityAligned) {
                primaryBlocker = AlignmentBlocker.VELOCITY;
            } else {
                primaryBlocker = AlignmentBlocker.NONE;
            }
            enforceable = primaryBlocker == AlignmentBlocker.NONE;
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

        public AlignmentBlocker getPrimaryBlocker() {
            return primaryBlocker;
        }

        public boolean isEnforceable() {
            return enforceable;
        }
    }

    public enum AlignmentBlocker {
        NONE,
        TELEPORT,
        BLOCK,
        VELOCITY,
        DEGRADED_PIPELINE
    }

    enum PendingWorldChangeType {
        TELEPORT,
        VELOCITY,
        BLOCK_CHANGE
    }

    static final class PendingWorldChange {
        private final PendingWorldChangeType type;
        private final long createdAtMillis;
        private final long expiresAtMillis;
        private final short anchorTransactionId;
        private final String reason;

        PendingWorldChange(PendingWorldChangeType type, long createdAtMillis, long expiresAtMillis,
                short anchorTransactionId, String reason) {
            this.type = type;
            this.createdAtMillis = createdAtMillis;
            this.expiresAtMillis = expiresAtMillis;
            this.anchorTransactionId = anchorTransactionId;
            this.reason = reason;
        }

        PendingWorldChangeType getType() {
            return type;
        }

        long getCreatedAtMillis() {
            return createdAtMillis;
        }

        long getExpiresAtMillis() {
            return expiresAtMillis;
        }

        short getAnchorTransactionId() {
            return anchorTransactionId;
        }

        String getReason() {
            return reason;
        }
    }
}
