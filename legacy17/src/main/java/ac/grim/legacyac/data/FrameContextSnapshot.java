package ac.grim.legacyac.data;

import ac.grim.legacyac.combat.HitboxFrame;
import ac.grim.legacyac.data.state.CompensationState;
import ac.grim.legacyac.tolerance.ToleranceBudgetEngine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable per-frame shared context used by movement/combat checks.
 */
public final class FrameContextSnapshot {
    private final long frameId;
    private final int txWindowId;
    private final PredictionOutputSnapshot predictionOutput;
    private final TxWindowStateSnapshot txWindowState;
    private final HitboxSnapshot targetHitboxSnapshot;
    private final ToleranceBudgetEngine.BudgetSnapshot budgetSnapshot;
    private final List<String> pendingBlockChanges;
    private final CompensationState.AlignmentBlocker alignmentBlocker;
    private final long actionWindowId;
    private final boolean enforceableFrame;

    public FrameContextSnapshot(long frameId, int txWindowId,
            PredictionOutputSnapshot predictionOutput,
            TxWindowStateSnapshot txWindowState,
            HitboxSnapshot targetHitboxSnapshot,
            ToleranceBudgetEngine.BudgetSnapshot budgetSnapshot,
            List<String> pendingBlockChanges,
            CompensationState.AlignmentBlocker alignmentBlocker,
            long actionWindowId,
            boolean enforceableFrame) {
        this.frameId = frameId;
        this.txWindowId = txWindowId;
        this.predictionOutput = predictionOutput;
        this.txWindowState = txWindowState;
        this.targetHitboxSnapshot = targetHitboxSnapshot;
        this.budgetSnapshot = budgetSnapshot;
        this.pendingBlockChanges = pendingBlockChanges == null ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(pendingBlockChanges));
        this.alignmentBlocker = alignmentBlocker == null ? CompensationState.AlignmentBlocker.NONE : alignmentBlocker;
        this.actionWindowId = actionWindowId;
        this.enforceableFrame = enforceableFrame;
    }

    public FrameContextSnapshot withPredictionOutput(PredictionOutputSnapshot output) {
        return new FrameContextSnapshot(frameId, txWindowId, output, txWindowState, targetHitboxSnapshot,
                budgetSnapshot, pendingBlockChanges, alignmentBlocker, actionWindowId, enforceableFrame);
    }

    public FrameContextSnapshot withTargetHitboxSnapshot(HitboxSnapshot hitboxSnapshot) {
        return new FrameContextSnapshot(frameId, txWindowId, predictionOutput, txWindowState, hitboxSnapshot,
                budgetSnapshot, pendingBlockChanges, alignmentBlocker, actionWindowId, enforceableFrame);
    }

    public long getFrameId() {
        return frameId;
    }

    public int getTxWindowId() {
        return txWindowId;
    }

    public PredictionOutputSnapshot getPredictionOutput() {
        return predictionOutput;
    }

    public TxWindowStateSnapshot getTxWindowState() {
        return txWindowState;
    }

    public HitboxSnapshot getTargetHitboxSnapshot() {
        return targetHitboxSnapshot;
    }

    public ToleranceBudgetEngine.BudgetSnapshot getBudgetSnapshot() {
        return budgetSnapshot;
    }

    public List<String> getPendingBlockChanges() {
        return pendingBlockChanges;
    }

    public CompensationState.AlignmentBlocker getAlignmentBlocker() {
        return alignmentBlocker;
    }

    public long getActionWindowId() {
        return actionWindowId;
    }

    public boolean isEnforceableFrame() {
        return enforceableFrame;
    }

    public static final class PredictionOutputSnapshot {
        private final boolean ready;
        private final double rawDeviation;
        private final double reducedDeviation;
        private final double horizontalDeviation;
        private final double reducedHorizontalDeviation;
        private final String bestProfile;

        public PredictionOutputSnapshot(boolean ready, double rawDeviation, double reducedDeviation,
                double horizontalDeviation, double reducedHorizontalDeviation, String bestProfile) {
            this.ready = ready;
            this.rawDeviation = rawDeviation;
            this.reducedDeviation = reducedDeviation;
            this.horizontalDeviation = horizontalDeviation;
            this.reducedHorizontalDeviation = reducedHorizontalDeviation;
            this.bestProfile = bestProfile == null ? "none" : bestProfile;
        }

        public boolean isReady() { return ready; }
        public double getRawDeviation() { return rawDeviation; }
        public double getReducedDeviation() { return reducedDeviation; }
        public double getHorizontalDeviation() { return horizontalDeviation; }
        public double getReducedHorizontalDeviation() { return reducedHorizontalDeviation; }
        public String getBestProfile() { return bestProfile; }
    }

    public static final class TxWindowStateSnapshot {
        private final short preTxId;
        private final short postTxId;
        private final int velocityFlags;
        private final int ticksObserved;

        public TxWindowStateSnapshot(short preTxId, short postTxId, int velocityFlags, int ticksObserved) {
            this.preTxId = preTxId;
            this.postTxId = postTxId;
            this.velocityFlags = velocityFlags;
            this.ticksObserved = ticksObserved;
        }

        public short getPreTxId() { return preTxId; }
        public short getPostTxId() { return postTxId; }
        public int getVelocityFlags() { return velocityFlags; }
        public int getTicksObserved() { return ticksObserved; }
    }

    public static final class HitboxSnapshot {
        private final long timestampMillis;
        private final double minX, minY, minZ;
        private final double maxX, maxY, maxZ;

        public HitboxSnapshot(long timestampMillis, double minX, double minY, double minZ,
                double maxX, double maxY, double maxZ) {
            this.timestampMillis = timestampMillis;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        public static HitboxSnapshot fromFrame(HitboxFrame frame) {
            if (frame == null) {
                return null;
            }
            return new HitboxSnapshot(frame.getTimestampMillis(), frame.getMinX(), frame.getMinY(), frame.getMinZ(),
                    frame.getMaxX(), frame.getMaxY(), frame.getMaxZ());
        }

        public long getTimestampMillis() { return timestampMillis; }
        public double getMinX() { return minX; }
        public double getMinY() { return minY; }
        public double getMinZ() { return minZ; }
        public double getMaxX() { return maxX; }
        public double getMaxY() { return maxY; }
        public double getMaxZ() { return maxZ; }
    }
}
