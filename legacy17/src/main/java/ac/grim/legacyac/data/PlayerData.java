package ac.grim.legacyac.data;

import ac.grim.legacyac.combat.HitboxFrame;
import ac.grim.legacyac.data.state.CombatState;
import ac.grim.legacyac.data.state.CompensationState;
import ac.grim.legacyac.data.state.EnforcementState;
import ac.grim.legacyac.data.state.EnvironmentState;
import ac.grim.legacyac.data.state.MovementState;
import ac.grim.legacyac.data.state.NetworkState;
import ac.grim.legacyac.debug.DetectionEvidence;
import ac.grim.legacyac.evidence.CombatEvidence;
import ac.grim.legacyac.tolerance.ToleranceBudgetEngine;
import ac.grim.legacyac.world.LegacyBlockState;
import ac.grim.legacyac.world.LegacyCompensatedWorld;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Per-player data holder  now delegates to 6 domain state aggregates.
 *
 * <p>
 * All existing public methods are preserved as "forwarding layer" so
 * that none of the check implementations break. New code should prefer
 * accessing the domain objects directly via {@code movement()},
 * {@code combat()}, {@code network()}, {@code compensation()},
 * {@code environment()}, {@code enforcement()}.
 * </p>
 */
public final class PlayerData {
    private final UUID uuid;
    private long joinAt;

    // ── Domain state aggregates (FR-1) ──────────────────────────────────
    private final MovementState movement = new MovementState();
    private final CombatState combat = new CombatState();
    private final NetworkState network = new NetworkState();
    private final CompensationState compensation = new CompensationState();
    private final EnvironmentState environment = new EnvironmentState();
    private final EnforcementState enforcement = new EnforcementState();
    private final LegacyCompensatedWorld compensatedWorld = new LegacyCompensatedWorld();

    // ── Tolerance budget snapshot (FR-3)  set once per frame ───────────
    private ToleranceBudgetEngine.BudgetSnapshot currentBudget;

    // ── Combat evidence buffer (FR-4) ──────────────────────────────────
    private final LinkedList<CombatEvidence> combatEvidenceBuffer = new LinkedList<CombatEvidence>();
    private static final int COMBAT_EVIDENCE_LIMIT = 80;

    // ── Legacy fields still needed directly ────────────────────────────
    // Velocity samples & knockback samples are kept here for backward compatibility
    private final LinkedList<VelocitySample> velocitySamples = new LinkedList<VelocitySample>();
    private final LinkedList<KnockbackSample> knockbackSamples = new LinkedList<KnockbackSample>();
    private static final int VELOCITY_SAMPLE_LIMIT = 12;
    private static final int KNOCKBACK_SAMPLE_LIMIT = 12;
    private KnockbackSample firstBreadKB;
    private KnockbackSample likelyKB;
    private double knockbackOffset;
    private short knockbackTransactionId;
    private boolean knockbackSetbackLike;

    // Inventory
    private boolean inventoryOpen;
    private long inventoryOpenAt;

    // Place/break/use windows
    private int placeWindow;
    private long placeWindowStart;
    private int breakWindow;
    private long breakWindowStart;
    private int useWindow;
    private long useWindowStart;

    // ── BadPackets state fields ──────────────────────────────────────────
    private int lastHeldSlot = -1;
    private int heldSlotChangeCount;
    private boolean lastSprintActionState;
    private int sprintActionCount;
    private boolean lastSneakActionState;
    private int sneakActionCount;
    private boolean recentRespawn;
    private int consecutiveLookOnlyPackets;
    private boolean diggingActive;

    // ── Timer state (nanosecond precision) ───────────────────────────────
    private long timerLastPacketNanos;
    private double timerBalance;

    // ── Scaffold state ───────────────────────────────────────────────────
    private long lastBlockPlaceTimeMs;
    private int sameTickPlaceCount;
    private long lastClientBlockPlacePacketAt;
    private int lastClientPlaceX;
    private int lastClientPlaceY;
    private int lastClientPlaceZ;
    private int lastClientPlaceFace = -1;
    private float lastClientCursorX;
    private float lastClientCursorY;
    private float lastClientCursorZ;
    private boolean hasLastClientCursor;
    private long lastUseItemPacketAt;
    private boolean usingItemPacketActive;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.joinAt = System.currentTimeMillis();
    }

    // ══════════════════════════════════════════════════════════════════
    // Domain accessors  new code should use these
    // ══════════════════════════════════════════════════════════════════

    public MovementState movement() {
        return movement;
    }

    public CombatState combat() {
        return combat;
    }

    public NetworkState network() {
        return network;
    }

    public CompensationState compensation() {
        return compensation;
    }

    public EnvironmentState environment() {
        return environment;
    }

    public EnforcementState enforcement() {
        return enforcement;
    }

    public LegacyCompensatedWorld world() {
        return compensatedWorld;
    }

    // ══════════════════════════════════════════════════════════════════
    // Tolerance Budget (FR-3)
    // ══════════════════════════════════════════════════════════════════

    public void setCurrentBudget(ToleranceBudgetEngine.BudgetSnapshot budget) {
        this.currentBudget = budget;
    }

    public ToleranceBudgetEngine.BudgetSnapshot getCurrentBudget() {
        return currentBudget;
    }

    // ══════════════════════════════════════════════════════════════════
    // Combat Evidence (FR-4)
    // ══════════════════════════════════════════════════════════════════

    public void recordCombatEvidence(CombatEvidence evidence) {
        if (evidence == null)
            return;
        combatEvidenceBuffer.addLast(evidence);
        while (combatEvidenceBuffer.size() > COMBAT_EVIDENCE_LIMIT) {
            combatEvidenceBuffer.removeFirst();
        }
    }

    public List<CombatEvidence> getCombatEvidenceSnapshot() {
        return Collections.unmodifiableList(new ArrayList<CombatEvidence>(combatEvidenceBuffer));
    }

    // ══════════════════════════════════════════════════════════════════
    // Forwarding layer  all legacy methods delegate to domain objects
    // ══════════════════════════════════════════════════════════════════

    public UUID getUuid() {
        return uuid;
    }

    // ── Movement ──

    public void handleMove(Player player, Location from, Location to, boolean onGround) {
        compensation.applyPendingWorldChanges();
        movement.onMove(from, to, onGround, player.isSprinting(), player.isSneaking());
        compensation.tickSlotSwitchGrace();

        double width = 0.6D;
        double height = player.isSneaking() ? 1.65D : 1.8D;
        boolean teleportMarker = System.currentTimeMillis() - compensation.getLastTeleportOrPearlAt() <= 400L;
        combat.recordHitbox(to.getX(), to.getY(), to.getZ(), width, height,
                teleportMarker, network.hasRecentTransactionAck(2000L), !compensation.isTeleportSyncPending());

        compensation.tickVelocityWindow(movement.getLastDeltaXZ(), movement.getLastDeltaY());

        compensation.getMovementStateSnapshot(); // refresh snapshot
        environment.tick(player, to, onGround, movement.getLastDeltaXZ(), movement.getLastDeltaY(),
                compensation.isTeleportSyncPending());
    }

    public boolean isMovementFrameInitialized() {
        return movement.isMovementFrameInitialized();
    }

    public void setMovementFrame(double x, double y, double z, float yaw, float pitch, long timestampNanos) {
        movement.setMovementFrame(x, y, z, yaw, pitch, timestampNanos);
    }

    public double getLastFrameX() {
        return movement.getLastFrameX();
    }

    public double getLastFrameY() {
        return movement.getLastFrameY();
    }

    public double getLastFrameZ() {
        return movement.getLastFrameZ();
    }

    public float getLastFrameYaw() {
        return movement.getLastFrameYaw();
    }

    public float getLastFramePitch() {
        return movement.getLastFramePitch();
    }

    public long getLastMovementFrameAtNanos() {
        return movement.getLastMovementFrameAtNanos();
    }

    public int getAirTicks() {
        return movement.getAirTicks();
    }

    public int getGroundTicks() {
        return movement.getGroundTicks();
    }

    public double getLastDeltaXZ() {
        return movement.getLastDeltaXZ();
    }

    public double getLastDeltaY() {
        return movement.getLastDeltaY();
    }

    public double getPrevDeltaXZ() {
        return movement.getPrevDeltaXZ();
    }

    public double getPrevPrevDeltaXZ() {
        return movement.getPrevPrevDeltaXZ();
    }

    public double getPrevPrevPrevDeltaXZ() {
        return movement.getPrevPrevPrevDeltaXZ();
    }

    public double getPrevDeltaY() {
        return movement.getPrevDeltaY();
    }

    public float getLastYaw() {
        return movement.getLastYaw();
    }

    public float getLastPitch() {
        return movement.getLastPitch();
    }

    public float getLastYawDelta() {
        return movement.getLastYawDelta();
    }

    public float getLastPitchDelta() {
        return movement.getLastPitchDelta();
    }

    public float getPrevYaw() {
        return movement.getPrevYaw();
    }

    public float getPrevPitch() {
        return movement.getPrevPitch();
    }

    public boolean wasSprinting() {
        return movement.wasSprinting();
    }

    public boolean wasOnGround() {
        return movement.wasOnGround();
    }

    public boolean isOnGroundNow() {
        return movement.isOnGroundNow();
    }

    public int getMoveWindow() {
        return movement.getMoveWindow();
    }

    public Location getLastSafeLocation() {
        return movement.getLastSafeLocation();
    }

    public void setLastSafeLocation(Location loc) {
        movement.updateSafeLocation(loc);
    }

    // ── Combat ──

    public long getLastAttackAt() {
        return combat.getLastAttackAt();
    }

    public void setLastAttackAt(long millis) {
        combat.setLastAttackAt(millis);
    }

    public int getLastAttackTargetId() {
        return combat.getLastAttackTargetId();
    }

    public void setLastAttackTargetId(int id) {
        combat.setLastAttackTargetId(id);
    }

    public int getClickWindow() {
        return combat.getClickWindow();
    }

    public void setClickWindow(int val) {
        combat.setClickWindow(val);
    }

    public long getClickWindowStart() {
        return combat.getClickWindowStart();
    }

    public void setClickWindowStart(long val) {
        combat.setClickWindowStart(val);
    }

    public void recordCurrentHitbox(double x, double y, double z) {
        combat.recordHitbox(x, y, z, 0.6D, 1.8D, false, network.hasRecentTransactionAck(2000L), true);
    }

    public void recordCurrentHitbox(double x, double y, double z, double width, double height) {
        combat.recordHitbox(x, y, z, width, height, false, network.hasRecentTransactionAck(2000L), true);
    }

    public void recordCurrentHitbox(double x, double y, double z, double width, double height, boolean teleportMarker) {
        combat.recordHitbox(x, y, z, width, height, teleportMarker, network.hasRecentTransactionAck(2000L),
                !compensation.isTeleportSyncPending());
    }

    public void recordCurrentHitbox(double x, double y, double z, double width, double height,
            boolean teleportMarker, boolean transactionAligned, boolean enforceable) {
        combat.recordHitbox(x, y, z, width, height, teleportMarker, transactionAligned, enforceable);
    }

    public List<HitboxFrame> getHitboxHistorySnapshot(long maxAgeMillis) {
        return combat.getHitboxHistorySnapshot(maxAgeMillis);
    }

    // ── Network ──

    public long getLastTransactionRttNanos() {
        return network.getLastTransactionRttNanos();
    }

    public long getLastTransTime() {
        return network.getLastTransTime();
    }

    public long getTransactionRttJitterNanos() {
        return network.getTransactionRttJitterNanos();
    }

    public long getLastRawMovementPacketAt() {
        return network.getLastRawMovementPacketAt();
    }

    public void setLastRawMovementPacketAt(long nanos) {
        network.setLastRawMovementPacketAt(nanos);
    }

    public long getLastServerPositionSyncAt() {
        return network.getLastServerPositionSyncAt();
    }

    public void setLastServerPositionSyncAt(long nanos) {
        network.setLastServerPositionSyncAt(nanos);
    }

    public int getRawMovementPacketCounter() {
        return network.getRawMovementPacketCounter();
    }

    public void incrementRawMovementPacketCounter() {
        network.incrementRawMovementPacketCounter();
    }

    public short nextTransactionActionId() {
        return network.nextTransactionActionId();
    }

    public void markTransactionSent(short actionId, long sentAtNanos) {
        network.markTransactionSent(actionId, sentAtNanos);
    }

    public void acknowledgeTransaction(short actionId, long recvAtNanos) {
        boolean found = network.acknowledgeTransaction(actionId, recvAtNanos);
        if (found) {
            compensation.getMovementStateSnapshot(); // refresh
        }
        onVelocityTransactionAck(actionId, recvAtNanos);
    }

    public void acknowledgeKeepAlive(long nowMillis) {
        network.acknowledgeKeepAlive(nowMillis);
    }

    public boolean hasRecentTransactionAck(long maxAgeMillis) {
        return network.hasRecentTransactionAck(maxAgeMillis);
    }

    public boolean hasRecentKeepAliveAck(long maxAgeMillis) {
        return network.hasRecentKeepAliveAck(maxAgeMillis);
    }

    public void clearPendingTransactions() {
        network.clearPendingTransactions();
    }

    // ── Compensation ──

    public long getLastTeleportAt() {
        return compensation.getLastTeleportAt();
    }

    public void setLastTeleportAt(long millis) {
        compensation.setLastTeleportAt(millis);
        environment.markTeleport();
    }

    public long getLastTeleportOrPearlAt() {
        return compensation.getLastTeleportOrPearlAt();
    }

    public void setLastTeleportOrPearlAt(long millis) {
        compensation.setLastTeleportOrPearlAt(millis);
    }

    public long getLastVelocityAt() {
        return compensation.getLastVelocityAt();
    }

    public void setLastVelocityAt(long millis) {
        compensation.setLastVelocityAt(millis);
        environment.markVelocity();
    }

    public double getLastVelocityXZ() {
        return compensation.getLastVelocityXZ();
    }

    public void setLastVelocityXZ(double val) {
        compensation.setLastVelocityXZ(val);
    }

    public boolean isMovementUnconfirmed() {
        return compensation.isMovementUnconfirmed();
    }

    public void setMovementUnconfirmed(boolean val) {
        compensation.setMovementUnconfirmed(val);
    }

    public boolean isTeleportSyncPending() {
        return compensation.isTeleportSyncPending();
    }

    public void beginTeleportSync(double x, double y, double z) {
        compensation.beginTeleportSync(x, y, z);
        movement.setMovementFrame(0, 0, 0, 0, 0, 0); // reset frame init
    }

    public void tryConfirmTeleportSync(double x, double y, double z) {
        compensation.tryConfirmTeleportSync(x, y, z, network.hasRecentTransactionAck(2000L));
    }

    public void armVelocityWindow(Vector velocity, int ticks) {
        compensation.armVelocityWindow(velocity.getX(), velocity.getZ(), velocity.getY(), ticks);
        environment.markVelocity();
    }

    public boolean hasPendingVelocityWindow() {
        return compensation.hasPendingVelocityWindow();
    }

    public boolean hasCompletedVelocityWindow() {
        return compensation.hasCompletedVelocityWindow();
    }

    public void clearVelocityWindow() {
        compensation.clearVelocityWindow();
    }

    public double getExpectedVelocityXZ() {
        return compensation.getExpectedVelocityXZ();
    }

    public double getExpectedVelocityY() {
        return compensation.getExpectedVelocityY();
    }

    public double getExpectedVelX() {
        return compensation.getExpectedVelX();
    }

    public double getExpectedVelZ() {
        return compensation.getExpectedVelZ();
    }

    public double getObservedVelocityXZ() {
        return compensation.getObservedVelocityXZ();
    }

    public double getObservedVelocityY() {
        return compensation.getObservedVelocityY();
    }

    public void recordPendingVelocityChange() {
        compensation.recordPendingVelocityChange(network.estimateOneWayDelayMillis());
    }

    public void recordPendingBlockChange(String reason) {
        compensation.recordPendingBlockChange(reason, network.estimateOneWayDelayMillis());
    }

    public void preloadCompensatedWorld(Player player, int radiusChunks) {
        compensatedWorld.preloadAround(player, radiusChunks);
    }

    public void queueCompensatedChunkRefresh(Player player, int chunkX, int chunkZ, String reason) {
        long delay = network.estimateOneWayDelayMillis();
        compensatedWorld.queueChunkRefresh(player.getWorld(), chunkX, chunkZ, delay);
        compensation.recordPendingBlockChange(reason, delay);
    }

    public void queueCompensatedBlockChange(Player player, int x, int y, int z, Material type, byte data, String reason) {
        long delay = network.estimateOneWayDelayMillis();
        compensatedWorld.queueBlockChange(player.getWorld(), x, y, z, type, data, delay);
        compensation.recordPendingBlockChange(reason, delay);
    }

    public LegacyBlockState getCompensatedBlockState(Player player, int x, int y, int z) {
        return compensatedWorld.getBlockState(player.getWorld(), x, y, z);
    }

    public Material getCompensatedBlockType(Player player, int x, int y, int z) {
        return compensatedWorld.getBlockType(player.getWorld(), x, y, z);
    }

    public byte getCompensatedBlockData(Player player, int x, int y, int z) {
        return compensatedWorld.getBlockData(player.getWorld(), x, y, z);
    }

    public int getPendingWorldChangesCount() {
        return compensation.getPendingWorldChangesCount();
    }

    public List<String> getPendingWorldChangeDebugSnapshot() {
        return compensation.getPendingWorldChangeDebugSnapshot();
    }

    public PlayerData.MovementStateSnapshot getMovementStateSnapshot() {
        return new PlayerData.MovementStateSnapshot(compensation.getMovementStateSnapshot());
    }

    public void markSlotSwitch() {
        compensation.markSlotSwitch();
    }

    public void startSlotSwitchGrace(int ticks) {
        compensation.startSlotSwitchGrace(ticks);
    }

    public boolean isInSlotSwitchGrace() {
        return compensation.isInSlotSwitchGrace();
    }

    public long getLastSlotSwitchAt() {
        return compensation.getLastSlotSwitchAt();
    }

    // ── Environment ──

    public PredictionContext getPredictionContext() {
        // Legacy compatibility wrapper
        return new PredictionContext(environment, this);
    }

    public String getScenarioTag() {
        return environment.getScenarioTag();
    }

    public void recordRodPull() {
        environment.markRodPull();
    }

    public void recordHighFallLanding() {
        environment.markHighFall();
    }

    public double getPredictionMinDeviation() {
        return environment.getPredictionMinDeviation();
    }

    public void setPredictionMinDeviation(double val) {
        environment.setPredictionMinDeviation(val);
    }

    public double getPredictionReducedDeviation() {
        return environment.getPredictionReducedDeviation();
    }

    public void setPredictionReducedDeviation(double val) {
        environment.setPredictionReducedDeviation(val);
    }

    public double getPredictionHorizontalDeviation() {
        return environment.getPredictionHorizontalDeviation();
    }

    public void setPredictionHorizontalDeviation(double val) {
        environment.setPredictionHorizontalDeviation(val);
    }

    public double getPredictionReducedHorizontalDeviation() {
        return environment.getPredictionReducedHorizontalDeviation();
    }

    public void setPredictionReducedHorizontalDeviation(double val) {
        environment.setPredictionReducedHorizontalDeviation(val);
    }

    public int getSpeedLevel() {
        return environment.getSpeedLevel();
    }

    public String getPredictionBestProfile() {
        return environment.getPredictionBestProfile();
    }

    public void setPredictionBestProfile(String val) {
        environment.setPredictionBestProfile(val);
    }

    public void giveOffsetLenienceNextTick(double offset) {
        environment.giveOffsetLenienceNextTick(offset);
    }

    public void removeOffsetLenience() {
        environment.removeOffsetLenience();
    }

    public double getLastHorizontalOffset() {
        return environment.getLastHorizontalOffset();
    }

    public double getLastVerticalOffset() {
        return environment.getLastVerticalOffset();
    }

    public void beginPredictionFrame(long frameTimestampNanos) {
        environment.beginPredictionFrame(frameTimestampNanos);
    }

    public void markPredictionReady(long frameTimestampNanos) {
        environment.markPredictionReady(frameTimestampNanos);
    }

    public boolean hasPredictionForFrame(long frameTimestampNanos) {
        return environment.hasPredictionForFrame(frameTimestampNanos);
    }

    public double getUsingItemConfidence() {
        return environment.getUsingItemConfidence();
    }

    public int getTicksUsingItem() {
        return environment.getTicksUsingItem();
    }

    public void updateUsingItemSignal(boolean candidateUsingItem) {
        environment.updateUsingItemSignal(candidateUsingItem);
    }

    public void resetNoSlowViolationStreak() {
        environment.resetNoSlowViolationStreak();
    }

    public int incrementNoSlowViolationStreak() {
        return environment.incrementNoSlowViolationStreak();
    }

    public boolean isParabolaAnomalous(double minAvgError, int minSamples) {
        return environment.isParabolaAnomalous(minAvgError, minSamples);
    }

    public void updateShadowPosition(double x, double y, double z, boolean onGround) {
        environment.updateShadowPosition(x, y, z, onGround);
    }

    public double getShadowDeviation() {
        return environment.getShadowDeviation();
    }

    public double getShadowMotionX() {
        return environment.getShadowMotionX();
    }

    public double getShadowMotionY() {
        return environment.getShadowMotionY();
    }

    public double getShadowMotionZ() {
        return environment.getShadowMotionZ();
    }

    public double getPrevShadowMotionX() {
        return environment.getPrevShadowMotionX();
    }

    public double getPrevShadowMotionY() {
        return environment.getPrevShadowMotionY();
    }

    public double getPrevShadowMotionZ() {
        return environment.getPrevShadowMotionZ();
    }

    public boolean isShadowInitialized() {
        return environment.isShadowInitialized();
    }

    // ── Enforcement ──

    public double addViolation(String check, double amount) {
        return enforcement.addViolation(check, amount);
    }

    public double getViolation(String check) {
        return enforcement.getViolation(check);
    }

    public double addBuffer(String check, double amount) {
        return enforcement.addBuffer(check, amount);
    }

    public double getBuffer(String check) {
        return enforcement.getBuffer(check);
    }

    public void setBuffer(String check, double value) {
        enforcement.setBuffer(check, value);
    }

    public double scaleBuffer(String check, double factor) {
        return enforcement.scaleBuffer(check, factor);
    }

    public boolean hasExecutedPunish(String check) {
        return enforcement.hasExecutedPunish(check);
    }

    public void markPunishExecuted(String check) {
        enforcement.markPunishExecuted(check);
    }

    public boolean isDebugEnabled() {
        return enforcement.isDebugEnabled();
    }

    public void setDebugEnabled(boolean val) {
        enforcement.setDebugEnabled(val);
    }

    public void setDetectionContext(String source, int tick) {
        enforcement.setDetectionContext(source, tick);
    }

    public String getDetectionSource() {
        return enforcement.getDetectionSource();
    }

    public int getDetectionTick() {
        return enforcement.getDetectionTick();
    }

    public void recordDetectionEvidence(DetectionEvidence evidence) {
        enforcement.recordDetectionEvidence(evidence);
    }

    public List<DetectionEvidence> getDetectionEvidenceSnapshot() {
        return enforcement.getDetectionEvidenceSnapshot();
    }

    public double getDetectionOffsetP95() {
        return enforcement.getDetectionOffsetP95();
    }

    public String getRecentTriggerChain(int limit) {
        return enforcement.getRecentTriggerChain(limit);
    }

    public void decayViolations(double amount) {
        enforcement.decayViolations(amount);
        combat.decayClickWindow();
    }

    // ── Join time ──

    public long getJoinAt() {
        return joinAt;
    }

    public void setJoinAt(long joinAt) {
        this.joinAt = joinAt;
    }

    // ── Inventory ──

    public boolean isInventoryOpen() {
        return inventoryOpen;
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

    // ── Place/break/use windows ──

    public int incrementPlaceWindow() {
        long now = System.currentTimeMillis();
        if (placeWindowStart == 0L || now - placeWindowStart > 1000L) {
            placeWindowStart = now;
            placeWindow = 0;
        }
        return ++placeWindow;
    }

    public int incrementBreakWindow() {
        long now = System.currentTimeMillis();
        if (breakWindowStart == 0L || now - breakWindowStart > 1000L) {
            breakWindowStart = now;
            breakWindow = 0;
        }
        return ++breakWindow;
    }

    public int incrementUseWindow() {
        long now = System.currentTimeMillis();
        if (useWindowStart == 0L || now - useWindowStart > 1000L) {
            useWindowStart = now;
            useWindow = 0;
        }
        return ++useWindow;
    }

    // ══════════════════════════════════════════════════════════════════
    // Velocity / Knockback sample management (kept in PlayerData for now)
    // ══════════════════════════════════════════════════════════════════

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

    public void startVelocitySample(long sentAtNanos, short preTxId, short postTxId, double vx, double vy, double vz,
            long txWindowMaxMs) {
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
        KnockbackSample sample = new KnockbackSample(sentAtNanos, entityId, preTxId, postTxId, vx, vy, vz, setbackLike,
                txWindowMaxMs);
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
            if (sample.isCompleted())
                continue;
            if (sample.isFirstBread())
                firstBreadKB = sample;
            if (sample.isLikely())
                likelyKB = sample;
            if (likelyKB != null)
                break;
        }
    }

    public void updateKnockbackOffset(double offset) {
        if (firstBreadKB != null)
            firstBreadKB.observe(offset);
        if (likelyKB != null)
            likelyKB.observe(offset);
        double bestOffset = offset;
        if (likelyKB != null)
            bestOffset = Math.min(bestOffset, likelyKB.getOffset());
        if (firstBreadKB != null)
            bestOffset = Math.min(bestOffset, firstBreadKB.getOffset());
        if (bestOffset < knockbackOffset || knockbackOffset == 0.0D) {
            knockbackOffset = Math.max(0.0D, bestOffset);
        }
    }

    public void recordKnockbackObservedMotion(double observedHorizontal, double responseThreshold,
            int delayedWindowTicks) {
        if (likelyKB != null)
            likelyKB.recordObservedMotion(observedHorizontal, responseThreshold, delayedWindowTicks);
        if (firstBreadKB != null)
            firstBreadKB.recordObservedMotion(observedHorizontal, responseThreshold, delayedWindowTicks);
    }

    public void completeCurrentKnockbackSample() {
        if (likelyKB != null)
            likelyKB.markCompleted();
        else if (firstBreadKB != null)
            firstBreadKB.markCompleted();
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

    public void setKnockbackOffset(double val) {
        this.knockbackOffset = val;
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

    // ══════════════════════════════════════════════════════════════════
    // Inner classes (legacy  kept for API compatibility)
    // ══════════════════════════════════════════════════════════════════

    /**
     * Legacy PredictionContext wrapper that delegates to EnvironmentState.
     */
    public static final class PredictionContext {
        private final EnvironmentState env;
        private final PlayerData data;

        PredictionContext(EnvironmentState env, PlayerData data) {
            this.env = env;
            this.data = data;
        }

        public PlayerData getData() {
            return data;
        }

        public boolean isRecentVelocity() {
            return env.isRecentVelocity();
        }

        public boolean isRecentRodPull() {
            return env.isRecentRodPull();
        }

        public boolean isInLiquid() {
            return env.isInLiquid();
        }

        public boolean isStuckEdge() {
            return env.isStuckEdge();
        }

        public boolean isRecentTeleport() {
            return env.isRecentTeleport();
        }

        public boolean isRecentHighFall() {
            return env.isRecentHighFall();
        }

        public boolean isNearGlitchyBlock() {
            return env.isNearGlitchyBlock();
        }

        public boolean isNearZeroThreeBoundary() {
            return env.isNearZeroThreeBoundary();
        }
        public boolean isRecentUnevenGround() {
            return env.isRecentUnevenGround();
        }

        public boolean isRecentSnowLayerGround() {
            return env.isRecentSnowLayerGround();
        }

        public boolean isNearPartialGround() {
            return env.isNearPartialGround();
        }


        public boolean isRecentEntityCollision() {
            return env.isRecentEntityCollision();
        }

        public String getScenarioTag() {
            return env.getScenarioTag();
        }

        public void markVelocity() {
            env.markVelocity();
        }

        public void markRodPull() {
            env.markRodPull();
        }

        public void markTeleport() {
            env.markTeleport();
        }

        public void markHighFall() {
            env.markHighFall();
        }

        // Stub for legacy calls that passed player/location to tick
        public void tick(Player player, Location to, boolean onGround, double deltaXZ, double deltaY,
                boolean teleportPending) {
            // Now handled by EnvironmentState.tick() called from handleMove()
        }
    }

    // ── MovementStateSnapshot compatibility ──
    // Old code referenced PlayerData.MovementStateSnapshot; now delegated
    public static final class MovementStateSnapshot {
        private final CompensationState.MovementStateSnapshot delegate;

        public MovementStateSnapshot(CompensationState.MovementStateSnapshot delegate) {
            this.delegate = delegate;
        }

        public boolean isTeleportAligned() {
            return delegate.isTeleportAligned();
        }

        public boolean isVelocityAligned() {
            return delegate.isVelocityAligned();
        }

        public boolean isBlockAligned() {
            return delegate.isBlockAligned();
        }

        public boolean isFullyAligned() {
            return delegate.isFullyAligned();
        }

        public int getPendingChanges() {
            return delegate.getPendingChanges();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // BadPackets state accessors
    // ══════════════════════════════════════════════════════════════════

    public int getLastHeldSlot() { return lastHeldSlot; }
    public void setLastHeldSlot(int slot) { lastHeldSlot = slot; }
    public int getHeldSlotChangeCount() { return heldSlotChangeCount; }
    public void incrementHeldSlotChangeCount() { heldSlotChangeCount++; }

    public boolean getLastSprintActionState() { return lastSprintActionState; }
    public void setLastSprintActionState(boolean state) { lastSprintActionState = state; }
    public int getSprintActionCount() { return sprintActionCount; }
    public void incrementSprintActionCount() { sprintActionCount++; }

    public boolean getLastSneakActionState() { return lastSneakActionState; }
    public void setLastSneakActionState(boolean state) { lastSneakActionState = state; }
    public int getSneakActionCount() { return sneakActionCount; }
    public void incrementSneakActionCount() { sneakActionCount++; }
    public boolean isRecentRespawn() { return recentRespawn; }
    public void markRecentRespawn() { recentRespawn = true; }
    public void clearRecentRespawn() { recentRespawn = false; }

    public int incrementConsecutiveLookOnlyPackets() { return ++consecutiveLookOnlyPackets; }
    public void resetConsecutiveLookOnlyPackets() { consecutiveLookOnlyPackets = 0; }

    public boolean isDiggingActive() { return diggingActive; }
    public void setDiggingActive(boolean val) { diggingActive = val; }

    // ══════════════════════════════════════════════════════════════════
    // Timer state accessors (nanosecond precision)
    // ══════════════════════════════════════════════════════════════════

    public long getTimerLastPacketNanos() { return timerLastPacketNanos; }
    public void setTimerLastPacketNanos(long nanos) { timerLastPacketNanos = nanos; }
    public double getTimerBalance() { return timerBalance; }
    public void setTimerBalance(double balance) { timerBalance = balance; }
    public void resetTimerState() { timerLastPacketNanos = 0L; timerBalance = 0.0; }

    // ══════════════════════════════════════════════════════════════════
    // Scaffold state accessors
    // ══════════════════════════════════════════════════════════════════

    public long getLastBlockPlaceTimeMs() { return lastBlockPlaceTimeMs; }
    public void setLastBlockPlaceTimeMs(long ms) { lastBlockPlaceTimeMs = ms; }
    public int incrementSameTickPlaceCount() { return ++sameTickPlaceCount; }
    public void resetSameTickPlaceCount() { sameTickPlaceCount = 0; }

    public void recordClientBlockPlacePacket(int x, int y, int z, int face, float cursorX, float cursorY, float cursorZ) {
        lastClientBlockPlacePacketAt = System.currentTimeMillis();
        lastClientPlaceX = x;
        lastClientPlaceY = y;
        lastClientPlaceZ = z;
        lastClientPlaceFace = face;
        lastClientCursorX = cursorX;
        lastClientCursorY = cursorY;
        lastClientCursorZ = cursorZ;
        hasLastClientCursor = true;
        lastUseItemPacketAt = lastClientBlockPlacePacketAt;
        usingItemPacketActive = true;
    }

    public long getLastClientBlockPlacePacketAt() { return lastClientBlockPlacePacketAt; }
    public int getLastClientPlaceX() { return lastClientPlaceX; }
    public int getLastClientPlaceY() { return lastClientPlaceY; }
    public int getLastClientPlaceZ() { return lastClientPlaceZ; }
    public int getLastClientPlaceFace() { return lastClientPlaceFace; }
    public float getLastClientCursorX() { return lastClientCursorX; }
    public float getLastClientCursorY() { return lastClientCursorY; }
    public float getLastClientCursorZ() { return lastClientCursorZ; }
    public boolean hasLastClientCursor() { return hasLastClientCursor; }
    public void clearLastClientCursor() { hasLastClientCursor = false; }
    public long getLastUseItemPacketAt() { return lastUseItemPacketAt; }
    public boolean isUsingItemPacketActive() { return usingItemPacketActive; }
    public void clearUsingItemPacket() { usingItemPacketActive = false; }
    public boolean hasRecentUseItemPacket(long maxAgeMillis) {
        return usingItemPacketActive && System.currentTimeMillis() - lastUseItemPacketAt <= maxAgeMillis;
    }

    // ══════════════════════════════════════════════════════════════════
    // KnockbackSample (unchanged inner class)
    // ══════════════════════════════════════════════════════════════════

    public static final class KnockbackSample {
        private final long expiresAtNanos;
        private final int entityId;
        private final short preTransactionId;
        private final short postTransactionId;
        private final double vx, vy, vz;
        private final boolean setbackLike;
        private boolean preAck, postAck, completed, delayedPattern;
        private double offset = Double.MAX_VALUE;
        private int ticksObserved, initialSilentTicks;
        private double maxObservedHorizontal;

        KnockbackSample(long sentAtNanos, int entityId, short preTransactionId, short postTransactionId,
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

        void handleAck(short actionId) {
            if (actionId == preTransactionId)
                preAck = true;
            if (actionId == postTransactionId)
                postAck = true;
        }

        void observe(double currentOffset) {
            if (!preAck || postAck)
                return;
            ticksObserved++;
            if (currentOffset < offset)
                offset = currentOffset;
        }

        void recordObservedMotion(double observedHorizontal, double responseThreshold, int delayedTicks) {
            if (!preAck || postAck)
                return;
            if (observedHorizontal > maxObservedHorizontal)
                maxObservedHorizontal = observedHorizontal;
            if (ticksObserved <= Math.max(1, delayedTicks) && observedHorizontal < responseThreshold)
                initialSilentTicks++;
            if (initialSilentTicks >= 1 && ticksObserved > Math.max(1, delayedTicks)
                    && observedHorizontal >= responseThreshold * 1.8D)
                delayedPattern = true;
        }

        boolean isExpired() {
            return System.nanoTime() > expiresAtNanos;
        }

        boolean isCompleted() {
            return completed || postAck;
        }

        boolean isFirstBread() {
            return preAck && !postAck;
        }

        boolean isLikely() {
            return preAck && postAck;
        }

        void markCompleted() {
            this.completed = true;
        }

        public int getEntityId() {
            return entityId;
        }

        public short getPreTransactionId() {
            return preTransactionId;
        }

        public short getPostTransactionId() {
            return postTransactionId;
        }

        public boolean isSetbackLike() {
            return setbackLike;
        }

        public double getOffset() {
            return offset == Double.MAX_VALUE ? 0.0D : offset;
        }

        public int getTicksObserved() {
            return ticksObserved;
        }

        public boolean isDelayedPattern() {
            return delayedPattern;
        }

        public double horizontalMagnitude() {
            return Math.sqrt((vx * vx) + (vz * vz));
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // VelocitySample (unchanged inner class)
    // ══════════════════════════════════════════════════════════════════

    public static final class VelocitySample {
        public static final int FLAG_PRE_ACK = 1;
        public static final int FLAG_POST_ACK = 1 << 1;
        public static final int FLAG_FIRST_CONFIRMED = 1 << 2;
        public static final int FLAG_LIKELY_CONFIRMED = 1 << 3;
        public static final int FLAG_DELAYED_KB_PATTERN = 1 << 4;

        private final long sentAtNanos;
        private final short preTxId, postTxId;
        private final double vx, vy, vz;
        private final long expiresAtNanos;
        private int stateFlags;
        private double minOffset = Double.MAX_VALUE;
        private int ticksObserved, ticksSincePreAck, initialSilentTicks;
        private double maxObservedHorizontal;

        VelocitySample(long sentAtNanos, short preTxId, short postTxId, double vx, double vy, double vz,
                long txWindowMaxMs) {
            this.sentAtNanos = sentAtNanos;
            this.preTxId = preTxId;
            this.postTxId = postTxId;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.expiresAtNanos = sentAtNanos + (Math.max(150L, txWindowMaxMs) * 1000000L);
        }

        void handleAck(short actionId, long recvAtNanos) {
            if (actionId == preTxId)
                stateFlags |= FLAG_PRE_ACK;
            if (actionId == postTxId)
                stateFlags |= FLAG_POST_ACK;
            if ((stateFlags & FLAG_PRE_ACK) != 0 && (stateFlags & FLAG_POST_ACK) != 0)
                stateFlags |= FLAG_LIKELY_CONFIRMED;
        }

        public void observeTick(double offset) {
            if ((stateFlags & FLAG_PRE_ACK) == 0 || (stateFlags & FLAG_POST_ACK) != 0)
                return;
            ticksObserved++;
            ticksSincePreAck++;
            if (offset < minOffset)
                minOffset = offset;
            if ((stateFlags & FLAG_FIRST_CONFIRMED) == 0)
                stateFlags |= FLAG_FIRST_CONFIRMED;
        }

        public void recordObservedMotion(double observedHorizontal, double responseThreshold, int delayedKbTicks) {
            if ((stateFlags & FLAG_PRE_ACK) == 0 || (stateFlags & FLAG_POST_ACK) != 0)
                return;
            if (observedHorizontal > maxObservedHorizontal)
                maxObservedHorizontal = observedHorizontal;
            if (ticksObserved <= Math.max(1, delayedKbTicks) && observedHorizontal < responseThreshold)
                initialSilentTicks++;
            if (initialSilentTicks >= 1 && ticksObserved > Math.max(1, delayedKbTicks)
                    && observedHorizontal >= responseThreshold * 1.8D)
                stateFlags |= FLAG_DELAYED_KB_PATTERN;
        }

        public boolean isExpired() {
            return System.nanoTime() > expiresAtNanos;
        }

        public boolean isCompleted() {
            return (stateFlags & FLAG_POST_ACK) != 0;
        }

        public long getSentAtNanos() {
            return sentAtNanos;
        }

        public short getPreTxId() {
            return preTxId;
        }

        public short getPostTxId() {
            return postTxId;
        }

        public double getVx() {
            return vx;
        }

        public double getVy() {
            return vy;
        }

        public double getVz() {
            return vz;
        }

        public double getMinOffset() {
            return minOffset == Double.MAX_VALUE ? 0.0D : minOffset;
        }

        public int getTicksObserved() {
            return ticksObserved;
        }

        public int getTicksSincePreAck() {
            return ticksSincePreAck;
        }

        public int getStateFlags() {
            return stateFlags;
        }

        public int getInitialSilentTicks() {
            return initialSilentTicks;
        }

        public double getMaxObservedHorizontal() {
            return maxObservedHorizontal;
        }

        public boolean hasFlag(int flag) {
            return (stateFlags & flag) != 0;
        }

        public void addFlag(int flag) {
            stateFlags |= flag;
        }
    }
}









