package ac.grim.legacyac.data;

import ac.grim.legacyac.combat.HitboxFrame;
import ac.grim.legacyac.debug.DetectionEvidence;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.Material;
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
    private long lastTeleportOrPearlAt;
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
    private double prevPrevDeltaXZ;
    private double prevPrevPrevDeltaXZ;
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
    private final LinkedList<VelocitySample> velocitySamples = new LinkedList<VelocitySample>();
    private final LinkedList<KnockbackSample> knockbackSamples = new LinkedList<KnockbackSample>();
    private static final int VELOCITY_SAMPLE_LIMIT = 12;
    private static final int KNOCKBACK_SAMPLE_LIMIT = 12;
    private KnockbackSample firstBreadKB;
    private KnockbackSample likelyKB;
    private double knockbackOffset;
    private short knockbackTransactionId;
    private boolean knockbackSetbackLike;
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
    private final LinkedList<PendingWorldChange> pendingWorldChanges = new LinkedList<PendingWorldChange>();
    private final MovementStateSnapshot movementStateSnapshot = new MovementStateSnapshot();
    private final LinkedList<DetectionEvidence> detectionEvidenceBuffer = new LinkedList<DetectionEvidence>();
    private static final int EVIDENCE_BUFFER_LIMIT = 160;
    private String detectionSource = "UNKNOWN";
    private int detectionTick;
    private final PredictionContext predictionContext = new PredictionContext();
    private double predictionMinDeviation;
    private double predictionReducedDeviation;
    private double predictionHorizontalDeviation;
    private double predictionReducedHorizontalDeviation;
    private String predictionBestProfile = "none";
    private double usingItemConfidence;
    private int ticksUsingItem;
    private long lastSlotSwitchAt;
    private int slotSwitchGraceTicksRemaining;
    private int noSlowConsecutiveViolationTicks;


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
        applyPendingWorldChanges();
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        prevPrevPrevDeltaXZ = prevPrevDeltaXZ;
        prevPrevDeltaXZ = prevDeltaXZ;
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

        if (slotSwitchGraceTicksRemaining > 0) {
            slotSwitchGraceTicksRemaining--;
        }

        long now = System.currentTimeMillis();
        if (moveWindowStart == 0L || now - moveWindowStart > 1000L) {
            moveWindowStart = now;
            moveWindow = 0;
        }
        moveWindow++;

        double width = 0.6D;
        double height = player.isSneaking() ? 1.65D : 1.8D;
        boolean teleportMarker = System.currentTimeMillis() - lastTeleportOrPearlAt <= 400L;
        recordCurrentHitbox(to.getX(), to.getY(), to.getZ(), width, height, teleportMarker);

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

        movementStateSnapshot.updateFrom(this, System.currentTimeMillis());
        predictionContext.tick(player, to, onGround, lastDeltaXZ, lastDeltaY, teleportSyncPending);
    }

    private void applyPendingWorldChanges() {
        long now = System.currentTimeMillis();
        Iterator<PendingWorldChange> iterator = pendingWorldChanges.iterator();
        while (iterator.hasNext()) {
            PendingWorldChange change = iterator.next();
            if (change.getEffectiveAtMillis() > now) {
                continue;
            }
            if (change.getType() == PendingWorldChangeType.TELEPORT) {
                movementUnconfirmed = false;
            }
            iterator.remove();
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
        predictionContext.markVelocity();
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
        this.lastTeleportOrPearlAt = lastTeleportAt;
        predictionContext.markTeleport();
    }

    public long getLastTeleportOrPearlAt() {
        return lastTeleportOrPearlAt;
    }

    public void setLastTeleportOrPearlAt(long lastTeleportOrPearlAt) {
        this.lastTeleportOrPearlAt = lastTeleportOrPearlAt;
    }

    public long getLastVelocityAt() {
        return lastVelocityAt;
    }

    public PredictionContext getPredictionContext() {
        return predictionContext;
    }

    public String getScenarioTag() {
        return predictionContext.getScenarioTag();
    }

    public double getPredictionMinDeviation() {
        return predictionMinDeviation;
    }

    public void setPredictionMinDeviation(double predictionMinDeviation) {
        this.predictionMinDeviation = Math.max(0.0D, predictionMinDeviation);
    }

    public double getPredictionReducedDeviation() {
        return predictionReducedDeviation;
    }

    public void setPredictionReducedDeviation(double predictionReducedDeviation) {
        this.predictionReducedDeviation = Math.max(0.0D, predictionReducedDeviation);
    }

    public String getPredictionBestProfile() {
        return predictionBestProfile;
    }

    public void setPredictionBestProfile(String predictionBestProfile) {
        this.predictionBestProfile = predictionBestProfile == null ? "none" : predictionBestProfile;
    }

    public double getPredictionHorizontalDeviation() {
        return predictionHorizontalDeviation;
    }

    public void setPredictionHorizontalDeviation(double predictionHorizontalDeviation) {
        this.predictionHorizontalDeviation = Math.max(0.0D, predictionHorizontalDeviation);
    }

    public double getPredictionReducedHorizontalDeviation() {
        return predictionReducedHorizontalDeviation;
    }

    public void setPredictionReducedHorizontalDeviation(double predictionReducedHorizontalDeviation) {
        this.predictionReducedHorizontalDeviation = Math.max(0.0D, predictionReducedHorizontalDeviation);
    }

    public double getUsingItemConfidence() {
        return usingItemConfidence;
    }

    public int getTicksUsingItem() {
        return ticksUsingItem;
    }

    public void updateUsingItemSignal(boolean candidateUsingItem) {
        if (candidateUsingItem) {
            usingItemConfidence = Math.min(1.0D, usingItemConfidence + 0.5D);
        } else {
            usingItemConfidence = Math.max(0.0D, usingItemConfidence - 0.4D);
        }

        if (usingItemConfidence >= 0.6D) {
            ticksUsingItem++;
        } else {
            ticksUsingItem = 0;
        }
    }

    public void resetNoSlowViolationStreak() {
        noSlowConsecutiveViolationTicks = 0;
    }

    public int incrementNoSlowViolationStreak() {
        noSlowConsecutiveViolationTicks++;
        return noSlowConsecutiveViolationTicks;
    }

    public void markSlotSwitch() {
        lastSlotSwitchAt = System.currentTimeMillis();
    }

    public void startSlotSwitchGrace(int ticks) {
        if (ticks > slotSwitchGraceTicksRemaining) {
            slotSwitchGraceTicksRemaining = ticks;
        }
    }

    public boolean isInSlotSwitchGrace() {
        return slotSwitchGraceTicksRemaining > 0;
    }

    public long getLastSlotSwitchAt() {
        return lastSlotSwitchAt;
    }

    public void recordRodPull() {
        predictionContext.markRodPull();
    }

    public void recordHighFallLanding() {
        predictionContext.markHighFall();
    }

    public void setLastVelocityAt(long lastVelocityAt) {
        this.lastVelocityAt = lastVelocityAt;
        predictionContext.markVelocity();
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

    public void setDetectionContext(String source, int tick) {
        this.detectionSource = source == null ? "UNKNOWN" : source;
        this.detectionTick = tick;
    }

    public String getDetectionSource() {
        return detectionSource;
    }

    public int getDetectionTick() {
        return detectionTick;
    }

    public void recordDetectionEvidence(DetectionEvidence evidence) {
        if (evidence == null) {
            return;
        }
        detectionEvidenceBuffer.addLast(evidence);
        while (detectionEvidenceBuffer.size() > EVIDENCE_BUFFER_LIMIT) {
            detectionEvidenceBuffer.removeFirst();
        }
    }

    public java.util.List<DetectionEvidence> getDetectionEvidenceSnapshot() {
        return Collections.unmodifiableList(new ArrayList<DetectionEvidence>(detectionEvidenceBuffer));
    }

    public double getDetectionOffsetP95() {
        if (detectionEvidenceBuffer.isEmpty()) {
            return 0.0D;
        }
        java.util.List<Double> offsets = new ArrayList<Double>();
        for (DetectionEvidence evidence : detectionEvidenceBuffer) {
            offsets.add(Double.valueOf(Math.max(0.0D, evidence.getOffset())));
        }
        Collections.sort(offsets);
        int index = (int) Math.ceil(offsets.size() * 0.95D) - 1;
        if (index < 0) {
            index = 0;
        }
        if (index >= offsets.size()) {
            index = offsets.size() - 1;
        }
        return offsets.get(index).doubleValue();
    }

    public String getRecentTriggerChain(int limit) {
        if (detectionEvidenceBuffer.isEmpty()) {
            return "none";
        }
        StringBuilder builder = new StringBuilder();
        int start = Math.max(0, detectionEvidenceBuffer.size() - Math.max(1, limit));
        for (int i = start; i < detectionEvidenceBuffer.size(); i++) {
            DetectionEvidence evidence = detectionEvidenceBuffer.get(i);
            if (builder.length() > 0) {
                builder.append(" -> ");
            }
            builder.append(evidence.getCheck());
            builder.append('@');
            builder.append(evidence.getTick());
        }
        return builder.toString();
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

    public double getPrevPrevDeltaXZ() {
        return prevPrevDeltaXZ;
    }

    public double getPrevPrevPrevDeltaXZ() {
        return prevPrevPrevDeltaXZ;
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
            movementStateSnapshot.updateFrom(this, lastTransTime);
        }
        onVelocityTransactionAck(actionId, recvAtNanos);
    }

    private void onVelocityTransactionAck(short actionId, long recvAtNanos) {
        for (VelocitySample sample : velocitySamples) {
            sample.handleAck(actionId, recvAtNanos);
        }
        for (KnockbackSample sample : knockbackSamples) {
            sample.handleAck(actionId);
        }
        pruneVelocitySamples();
        pruneKnockbackSamples();
    }

    public void startVelocitySample(long sentAtNanos, short preTxId, short postTxId, double vx, double vy, double vz, long txWindowMaxMs) {
        VelocitySample sample = new VelocitySample(sentAtNanos, preTxId, postTxId, vx, vy, vz, txWindowMaxMs);
        velocitySamples.addLast(sample);
        while (velocitySamples.size() > VELOCITY_SAMPLE_LIMIT) {
            velocitySamples.removeFirst();
        }
    }

    public VelocitySample getCurrentVelocitySample() {
        pruneVelocitySamples();
        return velocitySamples.isEmpty() ? null : velocitySamples.getLast();
    }

    public int getVelocitySampleQueueSize() {
        pruneVelocitySamples();
        return velocitySamples.size();
    }

    private void pruneVelocitySamples() {
        Iterator<VelocitySample> iterator = velocitySamples.iterator();
        while (iterator.hasNext()) {
            VelocitySample sample = iterator.next();
            if (sample.isExpired() || sample.isCompleted()) {
                if (sample != getCurrentVelocitySampleUnsafe()) {
                    iterator.remove();
                }
            }
        }
        while (velocitySamples.size() > VELOCITY_SAMPLE_LIMIT) {
            velocitySamples.removeFirst();
        }
    }

    private VelocitySample getCurrentVelocitySampleUnsafe() {
        return velocitySamples.isEmpty() ? null : velocitySamples.getLast();
    }


    public void startKnockbackSample(long sentAtNanos, int entityId, short preTxId, short postTxId,
                                     double vx, double vy, double vz, boolean setbackLike, long txWindowMaxMs) {
        KnockbackSample sample = new KnockbackSample(sentAtNanos, entityId, preTxId, postTxId, vx, vy, vz, setbackLike, txWindowMaxMs);
        knockbackSamples.addLast(sample);
        while (knockbackSamples.size() > KNOCKBACK_SAMPLE_LIMIT) {
            knockbackSamples.removeFirst();
        }
        firstBreadKB = null;
        likelyKB = null;
        knockbackTransactionId = postTxId;
        knockbackSetbackLike = setbackLike;
    }

    public void updateKnockbackStages() {
        firstBreadKB = null;
        likelyKB = null;
        for (KnockbackSample sample : knockbackSamples) {
            if (sample.isCompleted()) {
                continue;
            }
            if (sample.isFirstBread()) {
                firstBreadKB = sample;
            }
            if (sample.isLikely()) {
                likelyKB = sample;
            }
            if (likelyKB != null) {
                break;
            }
        }
    }

    public void updateKnockbackOffset(double offset) {
        if (firstBreadKB != null) {
            firstBreadKB.observe(offset);
        }
        if (likelyKB != null) {
            likelyKB.observe(offset);
        }
        double bestOffset = offset;
        if (likelyKB != null) {
            bestOffset = Math.min(bestOffset, likelyKB.getOffset());
        }
        if (firstBreadKB != null) {
            bestOffset = Math.min(bestOffset, firstBreadKB.getOffset());
        }
        if (bestOffset < knockbackOffset || knockbackOffset == 0.0D) {
            knockbackOffset = Math.max(0.0D, bestOffset);
        }
    }

    public void recordKnockbackObservedMotion(double observedHorizontal, double responseThreshold, int delayedWindowTicks) {
        if (likelyKB != null) {
            likelyKB.recordObservedMotion(observedHorizontal, responseThreshold, delayedWindowTicks);
        }
        if (firstBreadKB != null) {
            firstBreadKB.recordObservedMotion(observedHorizontal, responseThreshold, delayedWindowTicks);
        }
    }

    public void completeCurrentKnockbackSample() {
        if (likelyKB != null) {
            likelyKB.markCompleted();
        } else if (firstBreadKB != null) {
            firstBreadKB.markCompleted();
        }
        pruneKnockbackSamples();
        firstBreadKB = null;
        likelyKB = null;
    }

    public void pruneKnockbackSamples() {
        Iterator<KnockbackSample> iterator = knockbackSamples.iterator();
        while (iterator.hasNext()) {
            KnockbackSample sample = iterator.next();
            if (sample.isExpired() || sample.isCompleted()) {
                iterator.remove();
            }
        }
        while (knockbackSamples.size() > KNOCKBACK_SAMPLE_LIMIT) {
            knockbackSamples.removeFirst();
        }
    }

    public KnockbackSample getFirstBreadKB() {
        updateKnockbackStages();
        return firstBreadKB;
    }

    public KnockbackSample getLikelyKB() {
        updateKnockbackStages();
        return likelyKB;
    }

    public double getKnockbackOffset() {
        return knockbackOffset;
    }

    public void setKnockbackOffset(double knockbackOffset) {
        this.knockbackOffset = knockbackOffset;
    }

    public void addKnockbackScore(double amount) {
        this.knockbackOffset += Math.max(0.0D, amount);
    }

    public void decayKnockbackScore(double multiplier) {
        this.knockbackOffset *= Math.max(0.0D, Math.min(1.0D, multiplier));
    }

    public short getKnockbackTransactionId() {
        return knockbackTransactionId;
    }

    public boolean isKnockbackSetbackLike() {
        return knockbackSetbackLike;
    }

    public static final class KnockbackSample {
        private final long expiresAtNanos;
        private final int entityId;
        private final short preTransactionId;
        private final short postTransactionId;
        private final double vx;
        private final double vy;
        private final double vz;
        private final boolean setbackLike;
        private boolean preAck;
        private boolean postAck;
        private boolean completed;
        private boolean delayedPattern;
        private double offset = Double.MAX_VALUE;
        private int ticksObserved;
        private int initialSilentTicks;
        private double maxObservedHorizontal;

        private KnockbackSample(long sentAtNanos, int entityId, short preTransactionId, short postTransactionId,
                                double vx, double vy, double vz, boolean setbackLike, long txWindowMaxMs) {
            this.entityId = entityId;
            this.preTransactionId = preTransactionId;
            this.postTransactionId = postTransactionId;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.setbackLike = setbackLike;
            this.expiresAtNanos = sentAtNanos + (Math.max(150L, txWindowMaxMs) * 1000000L);
        }

        private void handleAck(short actionId) {
            if (actionId == preTransactionId) {
                preAck = true;
            }
            if (actionId == postTransactionId) {
                postAck = true;
            }
        }

        private void observe(double currentOffset) {
            if (!preAck || postAck) {
                return;
            }
            ticksObserved++;
            if (currentOffset < offset) {
                offset = currentOffset;
            }
        }

        private void recordObservedMotion(double observedHorizontal, double responseThreshold, int delayedTicks) {
            if (!preAck || postAck) {
                return;
            }
            if (observedHorizontal > maxObservedHorizontal) {
                maxObservedHorizontal = observedHorizontal;
            }
            if (ticksObserved <= Math.max(1, delayedTicks) && observedHorizontal < responseThreshold) {
                initialSilentTicks++;
            }
            if (initialSilentTicks >= 1 && ticksObserved > Math.max(1, delayedTicks)
                && observedHorizontal >= responseThreshold * 1.8D) {
                delayedPattern = true;
            }
        }

        private boolean isExpired() { return System.nanoTime() > expiresAtNanos; }
        private boolean isCompleted() { return completed || postAck; }
        private boolean isFirstBread() { return preAck && !postAck; }
        private boolean isLikely() { return preAck && postAck; }
        private void markCompleted() { this.completed = true; }
        public int getEntityId() { return entityId; }
        public short getPreTransactionId() { return preTransactionId; }
        public short getPostTransactionId() { return postTransactionId; }
        public boolean isSetbackLike() { return setbackLike; }
        public double getOffset() { return offset == Double.MAX_VALUE ? 0.0D : offset; }
        public int getTicksObserved() { return ticksObserved; }
        public boolean isDelayedPattern() { return delayedPattern; }
        public double horizontalMagnitude() { return Math.sqrt((vx * vx) + (vz * vz)); }
    }

    public static final class VelocitySample {
        public static final int FLAG_PRE_ACK = 1;
        public static final int FLAG_POST_ACK = 1 << 1;
        public static final int FLAG_FIRST_CONFIRMED = 1 << 2;
        public static final int FLAG_LIKELY_CONFIRMED = 1 << 3;
        public static final int FLAG_DELAYED_KB_PATTERN = 1 << 4;

        private final long sentAtNanos;
        private final short preTxId;
        private final short postTxId;
        private final double vx;
        private final double vy;
        private final double vz;
        private final long expiresAtNanos;
        private int stateFlags;
        private double minOffset = Double.MAX_VALUE;
        private int ticksObserved;
        private int ticksSincePreAck;
        private int initialSilentTicks;
        private double maxObservedHorizontal;

        private VelocitySample(long sentAtNanos, short preTxId, short postTxId, double vx, double vy, double vz, long txWindowMaxMs) {
            this.sentAtNanos = sentAtNanos;
            this.preTxId = preTxId;
            this.postTxId = postTxId;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.expiresAtNanos = sentAtNanos + (Math.max(150L, txWindowMaxMs) * 1000000L);
        }

        private void handleAck(short actionId, long recvAtNanos) {
            if (actionId == preTxId) {
                stateFlags |= FLAG_PRE_ACK;
            }
            if (actionId == postTxId) {
                stateFlags |= FLAG_POST_ACK;
            }
            if ((stateFlags & FLAG_PRE_ACK) != 0 && (stateFlags & FLAG_POST_ACK) != 0) {
                stateFlags |= FLAG_LIKELY_CONFIRMED;
            }
        }

        public void observeTick(double offset) {
            if ((stateFlags & FLAG_PRE_ACK) == 0 || (stateFlags & FLAG_POST_ACK) != 0) {
                return;
            }
            ticksObserved++;
            ticksSincePreAck++;
            if (offset < minOffset) {
                minOffset = offset;
            }
            if ((stateFlags & FLAG_FIRST_CONFIRMED) == 0) {
                stateFlags |= FLAG_FIRST_CONFIRMED;
            }
        }


        public void recordObservedMotion(double observedHorizontal, double responseThreshold, int delayedKbTicks) {
            if ((stateFlags & FLAG_PRE_ACK) == 0 || (stateFlags & FLAG_POST_ACK) != 0) {
                return;
            }
            if (observedHorizontal > maxObservedHorizontal) {
                maxObservedHorizontal = observedHorizontal;
            }
            if (ticksObserved <= Math.max(1, delayedKbTicks) && observedHorizontal < responseThreshold) {
                initialSilentTicks++;
            }
            if (initialSilentTicks >= 1 && ticksObserved > Math.max(1, delayedKbTicks)
                && observedHorizontal >= responseThreshold * 1.8D) {
                stateFlags |= FLAG_DELAYED_KB_PATTERN;
            }
        }

        public boolean isExpired() {
            return System.nanoTime() > expiresAtNanos;
        }

        public boolean isCompleted() {
            return (stateFlags & FLAG_POST_ACK) != 0;
        }

        public long getSentAtNanos() { return sentAtNanos; }
        public short getPreTxId() { return preTxId; }
        public short getPostTxId() { return postTxId; }
        public double getVx() { return vx; }
        public double getVy() { return vy; }
        public double getVz() { return vz; }
        public double getMinOffset() { return minOffset == Double.MAX_VALUE ? 0.0D : minOffset; }
        public int getTicksObserved() { return ticksObserved; }
        public int getTicksSincePreAck() { return ticksSincePreAck; }
        public int getStateFlags() { return stateFlags; }
        public int getInitialSilentTicks() { return initialSilentTicks; }
        public double getMaxObservedHorizontal() { return maxObservedHorizontal; }
        public boolean hasFlag(int flag) { return (stateFlags & flag) != 0; }
        public void addFlag(int flag) { stateFlags |= flag; }
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
        lastTeleportOrPearlAt = lastTeleportAt;
        movementFrameInitialized = false;
        enqueuePendingWorldChange(PendingWorldChangeType.TELEPORT, "server-position-sync");
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
            // Keep movementUnconfirmed until the client-side effective delay has passed.
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

    public void recordPendingVelocityChange() {
        enqueuePendingWorldChange(PendingWorldChangeType.VELOCITY, "entity-velocity");
    }

    public void recordPendingBlockChange(String reason) {
        enqueuePendingWorldChange(PendingWorldChangeType.BLOCK_CHANGE, reason);
    }

    private void enqueuePendingWorldChange(PendingWorldChangeType type, String reason) {
        long now = System.currentTimeMillis();
        long effectiveAt = now + estimateOneWayDelayMillis();
        pendingWorldChanges.add(new PendingWorldChange(type, now, effectiveAt, reason));
        while (pendingWorldChanges.size() > 32) {
            pendingWorldChanges.removeFirst();
        }
        movementStateSnapshot.updateFrom(this, now);
    }

    private long estimateOneWayDelayMillis() {
        long rttNanos = lastTransactionRttNanos > 0L ? lastTransactionRttNanos : lastTransactionRttSampleNanos;
        long jitterNanos = transactionRttJitterNanos;
        if (rttNanos <= 0L) {
            return 80L;
        }
        long estimate = (rttNanos / 2L) + Math.min(jitterNanos, rttNanos / 3L);
        long millis = estimate / 1000000L;
        if (millis < 30L) {
            return 30L;
        }
        if (millis > 350L) {
            return 350L;
        }
        return millis;
    }

    public MovementStateSnapshot getMovementStateSnapshot() {
        movementStateSnapshot.updateFrom(this, System.currentTimeMillis());
        return movementStateSnapshot;
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

    private int countPendingChanges(PendingWorldChangeType type, long nowMillis) {
        int count = 0;
        for (PendingWorldChange change : pendingWorldChanges) {
            if (change.getType() == type && change.getEffectiveAtMillis() > nowMillis) {
                count++;
            }
        }
        return count;
    }

    public static final class MovementStateSnapshot {
        private boolean teleportAligned;
        private boolean velocityAligned;
        private boolean blockAligned;
        private int pendingChanges;

        private void updateFrom(PlayerData data, long nowMillis) {
            data.applyPendingWorldChanges();
            int pendingTeleport = data.countPendingChanges(PendingWorldChangeType.TELEPORT, nowMillis);
            int pendingVelocity = data.countPendingChanges(PendingWorldChangeType.VELOCITY, nowMillis);
            int pendingBlock = data.countPendingChanges(PendingWorldChangeType.BLOCK_CHANGE, nowMillis);
            this.teleportAligned = pendingTeleport == 0 && !data.teleportSyncPending;
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

    public static final class PredictionContext {
        private static final int RECENT_TICK_WINDOW = 10;
        private int recentVelocityTicks;
        private int recentRodPullTicks;
        private int recentTeleportTicks;
        private int recentHighFallTicks;
        private boolean inLiquid;
        private boolean stuckEdge;
        private boolean nearGlitchyBlock;
        private boolean nearZeroThreeBoundary;
        private int recentEntityCollisionTicks;

        private void tick(Player player, Location to, boolean onGround, double deltaXZ, double deltaY, boolean teleportPending) {
            if (recentVelocityTicks > 0) {
                recentVelocityTicks--;
            }
            if (recentRodPullTicks > 0) {
                recentRodPullTicks--;
            }
            if (recentTeleportTicks > 0) {
                recentTeleportTicks--;
            }
            if (recentHighFallTicks > 0) {
                recentHighFallTicks--;
            }
            if (recentEntityCollisionTicks > 0) {
                recentEntityCollisionTicks--;
            }

            Material feet = to.getBlock().getType();
            Material below = to.clone().add(0.0D, -1.0D, 0.0D).getBlock().getType();
            inLiquid = isLiquid(feet) || isLiquid(below);
            stuckEdge = onGround && deltaXZ < 0.02D && Math.abs(deltaY) < 0.06D && hasAdjacentDrop(to);
            nearGlitchyBlock = isGlitchy(feet) || isGlitchy(below);
            nearZeroThreeBoundary = nearPointThree(deltaXZ) || nearPointThree(Math.abs(deltaY));

            if (player.getNearbyEntities(0.6D, 0.8D, 0.6D).size() > 0) {
                recentEntityCollisionTicks = RECENT_TICK_WINDOW;
            }

            if (teleportPending) {
                markTeleport();
            }

            if (onGround && player.getFallDistance() > 3.5F) {
                markHighFall();
            }
        }

        public boolean isRecentVelocity() { return recentVelocityTicks > 0; }
        public boolean isRecentRodPull() { return recentRodPullTicks > 0; }
        public boolean isInLiquid() { return inLiquid; }
        public boolean isStuckEdge() { return stuckEdge; }
        public boolean isRecentTeleport() { return recentTeleportTicks > 0; }
        public boolean isRecentHighFall() { return recentHighFallTicks > 0; }
        public boolean isNearGlitchyBlock() { return nearGlitchyBlock; }
        public boolean isNearZeroThreeBoundary() { return nearZeroThreeBoundary; }
        public boolean isRecentEntityCollision() { return recentEntityCollisionTicks > 0; }

        public void markVelocity() { recentVelocityTicks = RECENT_TICK_WINDOW; }
        public void markRodPull() { recentRodPullTicks = RECENT_TICK_WINDOW; }
        public void markTeleport() { recentTeleportTicks = RECENT_TICK_WINDOW; }
        public void markHighFall() { recentHighFallTicks = RECENT_TICK_WINDOW; }

        public String getScenarioTag() {
            if (recentRodPullTicks > 0 && recentVelocityTicks > 0) {
                return "rod_double_pull";
            }
            if (recentRodPullTicks > 0) {
                return "rod_pull";
            }
            if (recentTeleportTicks > 0) {
                return "pearl_displacement";
            }
            if (recentHighFallTicks > 0) {
                return "high_fall_landing";
            }
            if (inLiquid && recentVelocityTicks > 0) {
                return "liquid_hit";
            }
            if (inLiquid) {
                return "liquid_movement";
            }
            if (nearGlitchyBlock) {
                return "near_glitchy_block";
            }
            if (nearZeroThreeBoundary) {
                return "point_three_boundary";
            }
            if (stuckEdge) {
                return "edge_stuck";
            }
            if (recentVelocityTicks > 0) {
                return "velocity_window";
            }
            return "normal";
        }

        private static boolean hasAdjacentDrop(Location location) {
            int baseY = location.getBlockY() - 1;
            int baseX = location.getBlockX();
            int baseZ = location.getBlockZ();
            return isAirLike(location.getWorld().getBlockAt(baseX + 1, baseY, baseZ).getType())
                    || isAirLike(location.getWorld().getBlockAt(baseX - 1, baseY, baseZ).getType())
                    || isAirLike(location.getWorld().getBlockAt(baseX, baseY, baseZ + 1).getType())
                    || isAirLike(location.getWorld().getBlockAt(baseX, baseY, baseZ - 1).getType());
        }

        private static boolean isAirLike(Material material) {
            return material == Material.AIR || isLiquid(material);
        }

        private static boolean isLiquid(Material material) {
            return material == Material.WATER || material == Material.STATIONARY_WATER
                    || material == Material.LAVA || material == Material.STATIONARY_LAVA;
        }

        private static boolean isGlitchy(Material material) {
            return material == Material.LADDER || material == Material.VINE
                    || material == Material.WEB || material == Material.SOUL_SAND;
        }

        private static boolean nearPointThree(double value) {
            return Math.abs(value - 0.03D) <= 0.005D || Math.abs(value - 0.06D) <= 0.005D;
        }
    }

    private static final class PendingWorldChange {
        private final PendingWorldChangeType type;
        private final long createdAtMillis;
        private final long effectiveAtMillis;
        private final String reason;

        private PendingWorldChange(PendingWorldChangeType type, long createdAtMillis, long effectiveAtMillis, String reason) {
            this.type = type;
            this.createdAtMillis = createdAtMillis;
            this.effectiveAtMillis = effectiveAtMillis;
            this.reason = reason;
        }

        public PendingWorldChangeType getType() {
            return type;
        }

        public long getCreatedAtMillis() {
            return createdAtMillis;
        }

        public long getEffectiveAtMillis() {
            return effectiveAtMillis;
        }

        public String getReason() {
            return reason;
        }
    }

    private enum PendingWorldChangeType {
        TELEPORT,
        VELOCITY,
        BLOCK_CHANGE
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
        recordCurrentHitbox(x, y, z, 0.6D, 1.8D, false);
    }

    public void recordCurrentHitbox(double x, double y, double z, double width, double height) {
        recordCurrentHitbox(x, y, z, width, height, false);
    }

    public void recordCurrentHitbox(double x, double y, double z, double width, double height, boolean teleportMarker) {
        // 1.7/1.8 clients get an extra 0.1 hitbox expansion (vanilla behavior, same as
        // Grim)
        double hitboxExpand = 0.1D;
        double halfWidth = (width * 0.5D) + hitboxExpand;
        long now = System.currentTimeMillis();
        hitboxHistory.addFirst(
                new HitboxFrame(now, teleportMarker, x - halfWidth, y, z - halfWidth, x + halfWidth, y + height, z + halfWidth));

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
