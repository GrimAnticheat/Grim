package ac.grim.legacyac.data.state;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Domain state aggregate for network-related data.
 * Tracks transaction RTT, jitter, keep-alive, raw packet counters, etc.
 */
public final class NetworkState {
    private final Map<Short, Long> pendingTransactions = new ConcurrentHashMap<Short, Long>();
    private short transactionActionCounter;
    private long lastTransactionRttNanos;
    private long lastTransactionRttSampleNanos;
    private long transactionRttJitterNanos;
    private long lastTransTime;
    private long lastKeepAliveTime;
    private long lastRawMovementPacketAt;
    private long lastServerPositionSyncAt;
    private int rawMovementPacketCounter;

    // ── Update methods ──────────────────────────────────────────────────

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

    /**
     * Process transaction acknowledgement, update RTT and jitter.
     * 
     * @return true if a matching pending transaction was found
     */
    public boolean acknowledgeTransaction(short actionId, long recvAtNanos) {
        Long sent = pendingTransactions.remove(Short.valueOf(actionId));
        if (sent == null) {
            return false;
        }
        lastTransactionRttNanos = recvAtNanos - sent.longValue();
        if (lastTransactionRttSampleNanos != 0L) {
            transactionRttJitterNanos = Math.abs(lastTransactionRttNanos - lastTransactionRttSampleNanos);
        }
        lastTransactionRttSampleNanos = lastTransactionRttNanos;
        lastTransTime = System.currentTimeMillis();
        return true;
    }

    public void acknowledgeKeepAlive(long nowMillis) {
        this.lastKeepAliveTime = nowMillis;
    }

    public void setLastRawMovementPacketAt(long nanos) {
        this.lastRawMovementPacketAt = nanos;
    }

    public void incrementRawMovementPacketCounter() {
        this.rawMovementPacketCounter++;
    }

    public void setLastServerPositionSyncAt(long nanos) {
        this.lastServerPositionSyncAt = nanos;
    }

    public void clearPendingTransactions() {
        pendingTransactions.clear();
    }

    // ── Read interface ──────────────────────────────────────────────────

    public long getLastTransactionRttNanos() {
        return lastTransactionRttNanos;
    }

    public long getLastTransTime() {
        return lastTransTime;
    }

    public long getTransactionRttJitterNanos() {
        return transactionRttJitterNanos;
    }

    public long getLastRawMovementPacketAt() {
        return lastRawMovementPacketAt;
    }

    public long getLastServerPositionSyncAt() {
        return lastServerPositionSyncAt;
    }

    public int getRawMovementPacketCounter() {
        return rawMovementPacketCounter;
    }

    public long getLastKeepAliveTime() {
        return lastKeepAliveTime;
    }

    public boolean hasRecentTransactionAck(long maxAgeMillis) {
        return lastTransTime != 0L && System.currentTimeMillis() - lastTransTime <= maxAgeMillis;
    }

    public boolean hasRecentKeepAliveAck(long maxAgeMillis) {
        return lastKeepAliveTime != 0L && System.currentTimeMillis() - lastKeepAliveTime <= maxAgeMillis;
    }

    /**
     * Estimate one-way network delay in milliseconds based on transaction RTT and
     * jitter.
     */
    public long estimateOneWayDelayMillis() {
        long rttNanos = lastTransactionRttNanos > 0L ? lastTransactionRttNanos : lastTransactionRttSampleNanos;
        long jitterNanos = transactionRttJitterNanos;
        if (rttNanos <= 0L) {
            return 80L;
        }
        long estimate = (rttNanos / 2L) + Math.min(jitterNanos, rttNanos / 3L);
        long millis = estimate / 1000000L;
        if (millis < 30L)
            return 30L;
        if (millis > 350L)
            return 350L;
        return millis;
    }

    public double getRttMillis() {
        return lastTransactionRttNanos / 1000000.0D;
    }

    public double getJitterMillis() {
        return transactionRttJitterNanos / 1000000.0D;
    }
}
