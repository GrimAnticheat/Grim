package ac.grim.legacyac.check;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Records a single-frame pipeline execution trace for debug/observability.
 *
 * <p>
 * Each frame, the CheckManager creates a PipelineTrace, appends entries
 * for every check that ran (or was skipped), and the final result can be
 * serialized to the debug log.
 * </p>
 */
public final class PipelineTrace {
    private final long timestampNanos;
    private final String playerName;
    private final List<Entry> entries = new ArrayList<Entry>();
    private long totalDurationNanos;

    public PipelineTrace(long timestampNanos, String playerName) {
        this.timestampNanos = timestampNanos;
        this.playerName = playerName;
    }

    public void addEntry(String checkName, CheckStage stage, Status status, long durationNanos, String reason) {
        entries.add(new Entry(checkName, stage, status, durationNanos, reason));
    }

    /**
     * Convenience overload for stage-level trace recording.
     * 
     * @param stage         the pipeline stage
     * @param description   human-readable description of what ran
     * @param durationNanos elapsed time for this stage
     * @param ran           true if the stage actually executed checks
     * @param skipReason    if ran is false, the reason; null otherwise
     */
    public void addEntry(CheckStage stage, String description, long durationNanos, boolean ran, String skipReason) {
        entries.add(new Entry(description, stage, ran ? Status.RAN : Status.SKIPPED, durationNanos,
                skipReason == null ? "" : skipReason));
    }

    public void setTotalDurationNanos(long nanos) {
        this.totalDurationNanos = nanos;
    }

    public List<Entry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * Produce a compact single-line summary for debug logs.
     */
    public String toSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("[PIPELINE] ").append(playerName);
        sb.append(" total=").append(String.format(Locale.ROOT, "%.2fms", totalDurationNanos / 1000000.0D));
        int ran = 0, skipped = 0;
        for (Entry e : entries) {
            if (e.status == Status.RAN)
                ran++;
            else
                skipped++;
        }
        sb.append(" ran=").append(ran).append(" skipped=").append(skipped);
        if (skipped > 0) {
            sb.append(" [skipped:");
            boolean first = true;
            for (Entry e : entries) {
                if (e.status != Status.RAN) {
                    if (!first)
                        sb.append(',');
                    sb.append(e.checkName).append('(').append(e.reason).append(')');
                    first = false;
                }
            }
            sb.append(']');
        }
        return sb.toString();
    }

    public enum Status {
        RAN, SKIPPED, DISABLED, EXEMPT
    }

    public static final class Entry {
        private final String checkName;
        private final CheckStage stage;
        private final Status status;
        private final long durationNanos;
        private final String reason;

        Entry(String checkName, CheckStage stage, Status status, long durationNanos, String reason) {
            this.checkName = checkName;
            this.stage = stage;
            this.status = status;
            this.durationNanos = durationNanos;
            this.reason = reason == null ? "" : reason;
        }

        public String getCheckName() {
            return checkName;
        }

        public CheckStage getStage() {
            return stage;
        }

        public Status getStatus() {
            return status;
        }

        public long getDurationNanos() {
            return durationNanos;
        }

        public String getReason() {
            return reason;
        }
    }
}
