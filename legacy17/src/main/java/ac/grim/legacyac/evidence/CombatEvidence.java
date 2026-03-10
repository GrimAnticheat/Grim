package ac.grim.legacyac.evidence;

import java.util.Locale;

/**
 * Unified combat evidence structure (FR-4).
 *
 * <p>
 * All combat checks (Reach, KillAura, Velocity) produce a CombatEvidence
 * instance that can be serialized into a single event report for human review.
 * </p>
 *
 * <p>
 * Fields are designed so that a single evidence instance covers:
 * </p>
 * <ul>
 * <li>Actor/target identity &amp; position</li>
 * <li>Attack timeline (local time, transaction-aligned time)</li>
 * <li>Historical hitbox hit window</li>
 * <li>Input rotation &amp; trajectory key points</li>
 * <li>Final scoring &amp; threshold</li>
 * </ul>
 */
public final class CombatEvidence {
    // ── Identity ──
    private final long timestampMillis;
    private final String actorName;
    private final String targetName;
    private final CombatCheckType checkType;

    // ── Positions ──
    private final double actorX, actorY, actorZ;
    private final double targetX, targetY, targetZ;
    private final float actorYaw, actorPitch;

    // ── Attack timeline ──
    private final long localAttackTimeMs;
    private final long transactionAlignedTimeMs;
    private final long boxTimeOffsetMs;

    // ── Hitbox ──
    private final double directDistance;
    private final double closestHitboxDistance;
    private final boolean hitboxIntersects;
    private final boolean teleportMarkerHit;
    private final boolean enforceableWindow;

    // ── Rotation / trajectory ──
    private final float yawDelta;
    private final float pitchDelta;
    private final double horizontalDelta;

    // ── Scoring ──
    private final double score;
    private final double threshold;
    private final boolean flagged;
    private final String detail;
    private final long frameId;
    private final int txWindowId;

    public CombatEvidence(Builder builder) {
        this.timestampMillis = builder.timestampMillis;
        this.actorName = builder.actorName;
        this.targetName = builder.targetName;
        this.checkType = builder.checkType;
        this.actorX = builder.actorX;
        this.actorY = builder.actorY;
        this.actorZ = builder.actorZ;
        this.targetX = builder.targetX;
        this.targetY = builder.targetY;
        this.targetZ = builder.targetZ;
        this.actorYaw = builder.actorYaw;
        this.actorPitch = builder.actorPitch;
        this.localAttackTimeMs = builder.localAttackTimeMs;
        this.transactionAlignedTimeMs = builder.transactionAlignedTimeMs;
        this.boxTimeOffsetMs = builder.boxTimeOffsetMs;
        this.directDistance = builder.directDistance;
        this.closestHitboxDistance = builder.closestHitboxDistance;
        this.hitboxIntersects = builder.hitboxIntersects;
        this.teleportMarkerHit = builder.teleportMarkerHit;
        this.enforceableWindow = builder.enforceableWindow;
        this.yawDelta = builder.yawDelta;
        this.pitchDelta = builder.pitchDelta;
        this.horizontalDelta = builder.horizontalDelta;
        this.score = builder.score;
        this.threshold = builder.threshold;
        this.flagged = builder.flagged;
        this.detail = builder.detail;
        this.frameId = builder.frameId;
        this.txWindowId = builder.txWindowId;
    }

    // ── Read interface ──────────────────────────────────────────────────

    public long getTimestampMillis() {
        return timestampMillis;
    }

    public String getActorName() {
        return actorName;
    }

    public String getTargetName() {
        return targetName;
    }

    public CombatCheckType getCheckType() {
        return checkType;
    }

    public double getActorX() {
        return actorX;
    }

    public double getActorY() {
        return actorY;
    }

    public double getActorZ() {
        return actorZ;
    }

    public double getTargetX() {
        return targetX;
    }

    public double getTargetY() {
        return targetY;
    }

    public double getTargetZ() {
        return targetZ;
    }

    public float getActorYaw() {
        return actorYaw;
    }

    public float getActorPitch() {
        return actorPitch;
    }

    public long getLocalAttackTimeMs() {
        return localAttackTimeMs;
    }

    public long getTransactionAlignedTimeMs() {
        return transactionAlignedTimeMs;
    }

    public long getBoxTimeOffsetMs() {
        return boxTimeOffsetMs;
    }

    public double getDirectDistance() {
        return directDistance;
    }

    public double getClosestHitboxDistance() {
        return closestHitboxDistance;
    }

    public boolean isHitboxIntersects() {
        return hitboxIntersects;
    }

    public boolean isTeleportMarkerHit() {
        return teleportMarkerHit;
    }

    public boolean isEnforceableWindow() {
        return enforceableWindow;
    }

    public float getYawDelta() {
        return yawDelta;
    }

    public float getPitchDelta() {
        return pitchDelta;
    }

    public double getHorizontalDelta() {
        return horizontalDelta;
    }

    public double getScore() {
        return score;
    }

    public double getThreshold() {
        return threshold;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public String getDetail() {
        return detail;
    }

    public long getFrameId() {
        return frameId;
    }

    public int getTxWindowId() {
        return txWindowId;
    }

    /**
     * Human-readable single-line report for debug logs and admin review.
     */
    public String toReport() {
        return String.format(Locale.ROOT,
                "[%s] %s -> %s | dist=%.2f hitbox=%.2f intersect=%b " +
                        "boxOffset=%dms yaw=%.1f pitch=%.1f hDelta=%.3f " +
                        "score=%.3f/%.3f flagged=%b frame=%d txWin=%d | %s",
                checkType.name(), actorName, targetName,
                directDistance, closestHitboxDistance, hitboxIntersects,
                boxTimeOffsetMs, yawDelta, pitchDelta, horizontalDelta,
                score, threshold, flagged, frameId, txWindowId, detail);
    }

    // ── Check type enum ─────────────────────────────────────────────────

    public enum CombatCheckType {
        REACH, KILLAURA, VELOCITY
    }

    // ── Builder ─────────────────────────────────────────────────────────

    public static Builder builder(CombatCheckType type, String actorName, String targetName) {
        return new Builder(type, actorName, targetName);
    }

    public static final class Builder {
        private final long timestampMillis = System.currentTimeMillis();
        private final String actorName;
        private final String targetName;
        private final CombatCheckType checkType;
        private double actorX, actorY, actorZ;
        private double targetX, targetY, targetZ;
        private float actorYaw, actorPitch;
        private long localAttackTimeMs;
        private long transactionAlignedTimeMs;
        private long boxTimeOffsetMs;
        private double directDistance;
        private double closestHitboxDistance;
        private boolean hitboxIntersects;
        private boolean teleportMarkerHit;
        private boolean enforceableWindow = true;
        private float yawDelta;
        private float pitchDelta;
        private double horizontalDelta;
        private double score;
        private double threshold;
        private boolean flagged;
        private String detail = "";
        private long frameId = -1L;
        private int txWindowId = -1;

        Builder(CombatCheckType checkType, String actorName, String targetName) {
            this.checkType = checkType;
            this.actorName = actorName;
            this.targetName = targetName;
        }

        public Builder actorPos(double x, double y, double z) {
            actorX = x;
            actorY = y;
            actorZ = z;
            return this;
        }

        public Builder targetPos(double x, double y, double z) {
            targetX = x;
            targetY = y;
            targetZ = z;
            return this;
        }

        public Builder actorLook(float yaw, float pitch) {
            actorYaw = yaw;
            actorPitch = pitch;
            return this;
        }

        public Builder localAttackTime(long ms) {
            localAttackTimeMs = ms;
            return this;
        }

        public Builder txAlignedTime(long ms) {
            transactionAlignedTimeMs = ms;
            return this;
        }

        public Builder boxTimeOffset(long ms) {
            boxTimeOffsetMs = ms;
            return this;
        }

        public Builder directDistance(double d) {
            directDistance = d;
            return this;
        }

        public Builder closestHitboxDistance(double d) {
            closestHitboxDistance = d;
            return this;
        }

        public Builder hitboxIntersects(boolean b) {
            hitboxIntersects = b;
            return this;
        }

        public Builder teleportMarkerHit(boolean b) {
            teleportMarkerHit = b;
            return this;
        }

        public Builder enforceableWindow(boolean b) {
            enforceableWindow = b;
            return this;
        }

        public Builder rotation(float yawDelta, float pitchDelta) {
            this.yawDelta = yawDelta;
            this.pitchDelta = pitchDelta;
            return this;
        }

        public Builder horizontalDelta(double d) {
            horizontalDelta = d;
            return this;
        }

        public Builder scoring(double score, double threshold, boolean flagged) {
            this.score = score;
            this.threshold = threshold;
            this.flagged = flagged;
            return this;
        }

        public Builder detail(String d) {
            detail = d == null ? "" : d;
            return this;
        }

        public Builder frameLink(long frameId, int txWindowId) {
            this.frameId = frameId;
            this.txWindowId = txWindowId;
            return this;
        }

        public CombatEvidence build() {
            return new CombatEvidence(this);
        }
    }
}
