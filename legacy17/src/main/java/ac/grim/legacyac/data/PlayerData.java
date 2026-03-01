package ac.grim.legacyac.data;

import ac.grim.legacyac.combat.HitboxFrame;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.entity.Player;
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
    private double lastVelocityXZ;
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
    private long inventoryOpenAt;
    private boolean debugEnabled;
    private boolean prevSprinting;
    private Location lastSafeLocation;
    private double lastDeltaXZ;
    private double lastDeltaY;
    private double prevDeltaXZ;
    private double prevDeltaY;
    private float lastYaw;
    private float lastPitch;
    private float lastYawDelta;
    private float lastPitchDelta;
    private float prevYaw;
    private float prevPitch;
    private int velocityTicksRemaining;
    private double expectedVelocityXZ;
    private double expectedVelocityY;
    private double observedVelocityXZ;
    private double observedVelocityY;
    private long lastRawMovementPacketAt;
    private long lastServerPositionSyncAt;
    private int rawMovementPacketCounter;
    private final Map<Short, Long> pendingTransactions = new ConcurrentHashMap<Short, Long>();
    private short transactionActionCounter;
    private long lastTransactionRttNanos;
    private long lastTransactionRttSampleNanos;
    private long transactionRttJitterNanos;
    private long lastTransTime;
    private long lastKeepAliveTime;
    private boolean movementUnconfirmed;
    private boolean shadowInitialized;
    private double shadowX;
    private double shadowY;
    private double shadowZ;
    private double shadowMotionX;
    private double shadowMotionY;
    private double shadowMotionZ;
    private double shadowDeviation;
    private final LinkedList<HitboxFrame> hitboxHistory = new LinkedList<HitboxFrame>();
    private final LinkedList<Double> recentDeltaY = new LinkedList<Double>();
    private boolean teleportSyncPending;
    private double pendingTeleportX;
    private double pendingTeleportY;
    private double pendingTeleportZ;
    private int lastAttackTargetId;
    private boolean movementFrameInitialized;
    private double lastFrameX;
    private double lastFrameY;
    private double lastFrameZ;
    private float lastFrameYaw;
    private float lastFramePitch;
    private long lastMovementFrameAtNanos;
    private boolean previousOnGround;
    private boolean currentOnGround;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.joinAt = System.currentTimeMillis();
    }

    public UUID getUuid() {
        return uuid;
    }


    public int getLastAttackTargetId() {
        return lastAttackTargetId;
    }

    public void setLastAttackTargetId(int id) {
        this.lastAttackTargetId = id;
    }

    public void handleMove(Player player, Location from, Location to, boolean onGround) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        prevDeltaXZ = lastDeltaXZ;
        prevDeltaY = lastDeltaY;
        lastDeltaXZ = Math.sqrt(dx * dx + dz * dz);
        lastDeltaY = to.getY() - from.getY();
        prevSprinting = player.isSprinting();

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

        double width = 0.6D;
        double height = player.isSneaking() ? 1.65D : 1.8D;
        recordCurrentHitbox(to.getX(), to.getY(), to.getZ(), width, height);

        recentDeltaY.addLast(Double.valueOf(lastDeltaY));
        if (recentDeltaY.size() > 10) {
            recentDeltaY.removeFirst();
        }

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

    public boolean isMovementFrameInitialized() {
        return movementFrameInitialized;
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

    private double expectedVelX;
    private double expectedVelZ;

    public void armVelocityWindow(Vector velocity, int ticks) {
        double vx = velocity.getX();
        double vz = velocity.getZ();
        expectedVelX = vx;
        expectedVelZ = vz;
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
        return velocityTicksRemaining <= 0 && (expectedVelocityXZ > 0.0D || expectedVelocityY > 0.0D);
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

    public double getExpectedVelX() { return expectedVelX; }
    public double getExpectedVelZ() { return expectedVelZ; }

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
        for (Iterator<Map.Entry<String, Double>> iterator = violations.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, Double> entry = iterator.next();
            double next = entry.getValue().doubleValue() - amount;
            if (next <= 0.0D) {
                iterator.remove();
            } else {
                entry.setValue(next);
            }
        }

        for (Iterator<Map.Entry<String, Double>> iterator = buffers.entrySet().iterator(); iterator.hasNext();) {
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

    public double getLastVelocityXZ() {
        return lastVelocityXZ;
    }

    public void setLastVelocityXZ(double lastVelocityXZ) {
        this.lastVelocityXZ = lastVelocityXZ;
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

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    public void setInventoryOpen(boolean inventoryOpen) {
        if (inventoryOpen && !this.inventoryOpen) {
            this.inventoryOpenAt = System.currentTimeMillis();
        }
        this.inventoryOpen = inventoryOpen;
    }

    public long getInventoryOpenAt() {
        return inventoryOpenAt;
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

    public double getPrevDeltaXZ() {
        return prevDeltaXZ;
    }

    public double getPrevDeltaY() {
        return prevDeltaY;
    }

    public float getLastYawDelta() {
        return lastYawDelta;
    }

    public float getLastPitchDelta() {
        return lastPitchDelta;
    }

    public float getLastYaw() {
        return lastYaw;
    }

    public float getLastPitch() {
        return lastPitch;
    }

    public float getPrevYaw() {
        return prevYaw;
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

    public float getPrevPitch() {
        return prevPitch;
    }

    public long getLastRawMovementPacketAt() {
        return lastRawMovementPacketAt;
    }

    public void setLastRawMovementPacketAt(long lastRawMovementPacketAt) {
        this.lastRawMovementPacketAt = lastRawMovementPacketAt;
    }

    public long getLastServerPositionSyncAt() {
        return lastServerPositionSyncAt;
    }

    public void setLastServerPositionSyncAt(long lastServerPositionSyncAt) {
        this.lastServerPositionSyncAt = lastServerPositionSyncAt;
    }

    public int getRawMovementPacketCounter() {
        return rawMovementPacketCounter;
    }

    public void incrementRawMovementPacketCounter() {
        this.rawMovementPacketCounter++;
    }

    public short nextTransactionActionId() {
        transactionActionCounter++;
        if (transactionActionCounter <= 0) {
            transactionActionCounter = 1;
        }
        return transactionActionCounter;
    }

    public void markTransactionSent(short actionId, long sentAtNanos) {
        pendingTransactions.put(Short.valueOf(actionId), Long.valueOf(sentAtNanos));
    }

    public void acknowledgeTransaction(short actionId, long recvAtNanos) {
        Long sent = pendingTransactions.remove(Short.valueOf(actionId));
        if (sent != null) {
            lastTransactionRttNanos = recvAtNanos - sent.longValue();
            if (lastTransactionRttSampleNanos != 0L) {
                transactionRttJitterNanos = Math.abs(lastTransactionRttNanos - lastTransactionRttSampleNanos);
            }
            lastTransactionRttSampleNanos = lastTransactionRttNanos;
            lastTransTime = System.currentTimeMillis();
        }
    }

    public long getLastTransactionRttNanos() {
        return lastTransactionRttNanos;
    }

    public long getLastTransTime() {
        return lastTransTime;
    }

    public void acknowledgeKeepAlive(long nowMillis) {
        this.lastKeepAliveTime = nowMillis;
    }

    public boolean hasRecentTransactionAck(long maxAgeMillis) {
        return lastTransTime != 0L && System.currentTimeMillis() - lastTransTime <= maxAgeMillis;
    }

    public boolean hasRecentKeepAliveAck(long maxAgeMillis) {
        return lastKeepAliveTime != 0L && System.currentTimeMillis() - lastKeepAliveTime <= maxAgeMillis;
    }

    public boolean isMovementUnconfirmed() {
        return movementUnconfirmed;
    }

    public void setMovementUnconfirmed(boolean movementUnconfirmed) {
        this.movementUnconfirmed = movementUnconfirmed;
    }

    public long getTransactionRttJitterNanos() {
        return transactionRttJitterNanos;
    }

    public void clearPendingTransactions() {
        pendingTransactions.clear();
    }

    public double scaleBuffer(String check, double factor) {
        double next = getBuffer(check) * factor;
        if (next <= 0.0001D) {
            buffers.remove(check);
            return 0.0D;
        }
        buffers.put(check, next);
        return next;
    }

    public void beginTeleportSync(double x, double y, double z) {
        teleportSyncPending = true;
        pendingTeleportX = x;
        pendingTeleportY = y;
        pendingTeleportZ = z;
        movementUnconfirmed = true;
        lastTeleportAt = System.currentTimeMillis();
        movementFrameInitialized = false;
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
        if (hasRecentTransactionAck(2000L)) {
            teleportSyncPending = false;
            movementUnconfirmed = false;
        }
    }

    public boolean isTeleportSyncPending() {
        if (teleportSyncPending) {
            // Safety: auto-clear if stuck for too long (e.g. transaction ack failed)
            long elapsed = System.currentTimeMillis() - lastTeleportAt;
            if (elapsed > 1500L) {
                teleportSyncPending = false;
                movementUnconfirmed = false;
            }
        }
        return teleportSyncPending;
    }

    public boolean isParabolaAnomalous(double minAvgError, int minSamples) {
        if (recentDeltaY.size() < minSamples) {
            return false;
        }

        double totalError = 0.0D;
        int compared = 0;
        Double previous = null;
        for (Double current : recentDeltaY) {
            if (previous != null) {
                double expected = (previous.doubleValue() - 0.08D) * 0.98D;
                totalError += Math.abs(current.doubleValue() - expected);
                compared++;
            }
            previous = current;
        }

        if (compared == 0) {
            return false;
        }

        double averageError = totalError / compared;
        return averageError >= minAvgError;
    }

    public void updateShadowPosition(double x, double y, double z, boolean onGround) {
        if (!shadowInitialized) {
            shadowInitialized = true;
            shadowX = x;
            shadowY = y;
            shadowZ = z;
            shadowMotionX = 0.0D;
            shadowMotionY = 0.0D;
            shadowMotionZ = 0.0D;
            shadowDeviation = 0.0D;
            return;
        }

        double friction = onGround ? (0.91D * 0.60D) : 0.91D;
        double expectedX = shadowX + (shadowMotionX * friction);
        double expectedY = shadowY + ((shadowMotionY - 0.08D) * 0.98D);
        double expectedZ = shadowZ + (shadowMotionZ * friction);

        double diffX = x - expectedX;
        double diffY = y - expectedY;
        double diffZ = z - expectedZ;
        shadowDeviation = Math.sqrt(diffX * diffX + diffY * diffY + diffZ * diffZ);

        shadowMotionX = x - shadowX;
        shadowMotionY = y - shadowY;
        shadowMotionZ = z - shadowZ;
        shadowX = x;
        shadowY = y;
        shadowZ = z;
    }

    public double getShadowDeviation() {
        return shadowDeviation;
    }

    public void recordCurrentHitbox(double x, double y, double z) {
        recordCurrentHitbox(x, y, z, 0.6D, 1.8D);
    }

    public void recordCurrentHitbox(double x, double y, double z, double width, double height) {
        // 1.7/1.8 clients get an extra 0.1 hitbox expansion (vanilla behavior, same as
        // Grim)
        double hitboxExpand = 0.1D;
        double halfWidth = (width * 0.5D) + hitboxExpand;
        long now = System.currentTimeMillis();
        hitboxHistory.addFirst(
                new HitboxFrame(now, x - halfWidth, y, z - halfWidth, x + halfWidth, y + height, z + halfWidth));

        while (!hitboxHistory.isEmpty() && now - hitboxHistory.getLast().getTimestampMillis() > 400L) {
            hitboxHistory.removeLast();
        }
    }

    public List<HitboxFrame> getHitboxHistorySnapshot(long maxAgeMillis) {
        long now = System.currentTimeMillis();
        LinkedList<HitboxFrame> copy = new LinkedList<HitboxFrame>();
        for (HitboxFrame frame : hitboxHistory) {
            if (now - frame.getTimestampMillis() <= maxAgeMillis) {
                copy.add(frame);
            }
        }
        return copy;
    }

}
