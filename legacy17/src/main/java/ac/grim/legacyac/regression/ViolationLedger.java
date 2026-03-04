package ac.grim.legacyac.regression;

import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.evidence.CombatEvidence;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Phase E (FR-5): Records violation events per-check and produces aggregate
 * regression statistics.
 *
 * <p>
 * Each flag() in a Check will call {@link #record(String, ViolationEntry)} to
 * log the event. The engine then provides read-only snapshot APIs for
 * generating reports, comparing baselines, and gate-checking release criteria.
 * </p>
 */
public final class ViolationLedger {

    // ── Per-check aggregate counters ────────────────────────────────────
    private final ConcurrentHashMap<String, CheckStats> statsMap = new ConcurrentHashMap<String, CheckStats>();

    // ── Recent breach window (ring buffer) ──────────────────────────────
    private final ViolationEntry[] recentWindow;
    private int recentIndex = 0;

    public ViolationLedger(int windowSize) {
        this.recentWindow = new ViolationEntry[Math.max(16, windowSize)];
    }

    public ViolationLedger() {
        this(512);
    }

    // ── Recording ───────────────────────────────────────────────────────

    public void record(String checkName, ViolationEntry entry) {
        CheckStats stats = statsMap.get(checkName);
        if (stats == null) {
            stats = new CheckStats(checkName);
            CheckStats existing = statsMap.putIfAbsent(checkName, stats);
            if (existing != null) {
                stats = existing;
            }
        }
        stats.record(entry);

        synchronized (recentWindow) {
            recentWindow[recentIndex % recentWindow.length] = entry;
            recentIndex++;
        }
    }

    // ── Read APIs ───────────────────────────────────────────────────────

    public CheckStats getStats(String checkName) {
        return statsMap.get(checkName);
    }

    public Map<String, CheckStats> getAllStats() {
        return Collections.unmodifiableMap(new HashMap<String, CheckStats>(statsMap));
    }

    /**
     * Produce a structured regression report covering all checks.
     */
    public RegressionReport generateReport() {
        Map<String, CheckStats> snapshot = getAllStats();
        long totalFlags = 0;
        long totalExempted = 0;
        double worstFPRate = 0.0D;
        String worstCheck = "";

        for (Map.Entry<String, CheckStats> e : snapshot.entrySet()) {
            CheckStats cs = e.getValue();
            totalFlags += cs.totalFlags.get();
            totalExempted += cs.totalExempted.get();
            double fpRate = cs.estimatedFalsePositiveRate();
            if (fpRate > worstFPRate) {
                worstFPRate = fpRate;
                worstCheck = e.getKey();
            }
        }

        return new RegressionReport(snapshot, totalFlags, totalExempted, worstCheck, worstFPRate);
    }

    /**
     * Get the N most recent violations across all checks.
     */
    public List<ViolationEntry> getRecentViolations(int count) {
        List<ViolationEntry> result = new ArrayList<ViolationEntry>();
        synchronized (recentWindow) {
            int start = Math.max(0, recentIndex - count);
            for (int i = start; i < recentIndex; i++) {
                ViolationEntry e = recentWindow[i % recentWindow.length];
                if (e != null) {
                    result.add(e);
                }
            }
        }
        return result;
    }

    // ── Per-check stats ─────────────────────────────────────────────────

    public static final class CheckStats {
        private final String checkName;
        private final AtomicLong totalFlags = new AtomicLong();
        private final AtomicLong totalExempted = new AtomicLong();
        private final AtomicLong withinGraceFlags = new AtomicLong(); // flags within join/teleport/velocity grace
        private volatile double maxScore;
        private volatile double avgScoreSum;
        private volatile long avgScoreCount;
        private volatile double avgRttMs;
        private volatile long avgRttCount;
        private volatile long firstFlagMs;
        private volatile long lastFlagMs;
        private volatile long maxTriggerLatencyMs; // max time from event to detection

        CheckStats(String checkName) {
            this.checkName = checkName;
        }

        void record(ViolationEntry entry) {
            long count = totalFlags.incrementAndGet();

            if (entry.isExempted()) {
                totalExempted.incrementAndGet();
            }
            if (entry.isWithinGrace()) {
                withinGraceFlags.incrementAndGet();
            }

            if (entry.getScore() > maxScore) {
                maxScore = entry.getScore();
            }
            avgScoreSum += entry.getScore();
            avgScoreCount++;

            if (entry.getRttMs() > 0.0D) {
                avgRttMs = ((avgRttMs * avgRttCount) + entry.getRttMs()) / (avgRttCount + 1);
                avgRttCount++;
            }

            long now = entry.getTimestampMs();
            if (firstFlagMs == 0L) {
                firstFlagMs = now;
            }
            lastFlagMs = now;

            if (entry.getTriggerLatencyMs() > maxTriggerLatencyMs) {
                maxTriggerLatencyMs = entry.getTriggerLatencyMs();
            }
        }

        public String getCheckName() {
            return checkName;
        }

        public long getTotalFlags() {
            return totalFlags.get();
        }

        public long getTotalExempted() {
            return totalExempted.get();
        }

        public long getWithinGraceFlags() {
            return withinGraceFlags.get();
        }

        public double getMaxScore() {
            return maxScore;
        }

        public double getAvgScore() {
            return avgScoreCount > 0 ? avgScoreSum / avgScoreCount : 0.0D;
        }

        public double getAvgRttMs() {
            return avgRttMs;
        }

        public long getMaxTriggerLatencyMs() {
            return maxTriggerLatencyMs;
        }

        /**
         * Estimate false-positive rate as the ratio of exempted flags to total flags.
         * A high rate suggests the check may be too sensitive.
         */
        public double estimatedFalsePositiveRate() {
            long total = totalFlags.get();
            if (total == 0) {
                return 0.0D;
            }
            return (double) totalExempted.get() / total;
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT,
                    "%s: flags=%d exempted=%d grace=%d fp≈%.1f%% maxScore=%.2f avgScore=%.2f avgRtt=%.0fms maxLatency=%dms",
                    checkName, totalFlags.get(), totalExempted.get(), withinGraceFlags.get(),
                    estimatedFalsePositiveRate() * 100.0D, maxScore, getAvgScore(), avgRttMs, maxTriggerLatencyMs);
        }
    }

    // ── Violation entry ─────────────────────────────────────────────────

    public static final class ViolationEntry {
        private final long timestampMs;
        private final String checkName;
        private final String playerName;
        private final double score;
        private final double buffer;
        private final double vl;
        private final double rttMs;
        private final String source;
        private final String detail;
        private final boolean exempted;
        private final boolean withinGrace;
        private final long triggerLatencyMs;
        private final String budgetTag; // budget scenario summary

        public ViolationEntry(Builder builder) {
            this.timestampMs = builder.timestampMs;
            this.checkName = builder.checkName;
            this.playerName = builder.playerName;
            this.score = builder.score;
            this.buffer = builder.buffer;
            this.vl = builder.vl;
            this.rttMs = builder.rttMs;
            this.source = builder.source;
            this.detail = builder.detail;
            this.exempted = builder.exempted;
            this.withinGrace = builder.withinGrace;
            this.triggerLatencyMs = builder.triggerLatencyMs;
            this.budgetTag = builder.budgetTag;
        }

        // Getters
        public long getTimestampMs() {
            return timestampMs;
        }

        public String getCheckName() {
            return checkName;
        }

        public String getPlayerName() {
            return playerName;
        }

        public double getScore() {
            return score;
        }

        public double getBuffer() {
            return buffer;
        }

        public double getVl() {
            return vl;
        }

        public double getRttMs() {
            return rttMs;
        }

        public String getSource() {
            return source;
        }

        public String getDetail() {
            return detail;
        }

        public boolean isExempted() {
            return exempted;
        }

        public boolean isWithinGrace() {
            return withinGrace;
        }

        public long getTriggerLatencyMs() {
            return triggerLatencyMs;
        }

        public String getBudgetTag() {
            return budgetTag;
        }

        /**
         * Single-line structured report suitable for log files and admin review.
         */
        public String toReportLine() {
            return String.format(Locale.ROOT,
                    "[%d] %s %s score=%.3f buf=%.2f vl=%.2f rtt=%.0fms src=%s lat=%dms budget=%s | %s",
                    timestampMs, playerName, checkName, score, buffer, vl, rttMs, source,
                    triggerLatencyMs, budgetTag, detail);
        }

        public static final class Builder {
            private long timestampMs = System.currentTimeMillis();
            private String checkName = "";
            private String playerName = "";
            private double score;
            private double buffer;
            private double vl;
            private double rttMs;
            private String source = "";
            private String detail = "";
            private boolean exempted;
            private boolean withinGrace;
            private long triggerLatencyMs;
            private String budgetTag = "";

            public Builder check(String name) {
                this.checkName = name;
                return this;
            }

            public Builder player(String name) {
                this.playerName = name;
                return this;
            }

            public Builder score(double s) {
                this.score = s;
                return this;
            }

            public Builder buffer(double b) {
                this.buffer = b;
                return this;
            }

            public Builder vl(double v) {
                this.vl = v;
                return this;
            }

            public Builder rttMs(double r) {
                this.rttMs = r;
                return this;
            }

            public Builder source(String s) {
                this.source = s;
                return this;
            }

            public Builder detail(String d) {
                this.detail = d;
                return this;
            }

            public Builder exempted(boolean e) {
                this.exempted = e;
                return this;
            }

            public Builder withinGrace(boolean g) {
                this.withinGrace = g;
                return this;
            }

            public Builder triggerLatencyMs(long l) {
                this.triggerLatencyMs = l;
                return this;
            }

            public Builder budgetTag(String t) {
                this.budgetTag = t == null ? "" : t;
                return this;
            }

            public ViolationEntry build() {
                return new ViolationEntry(this);
            }
        }
    }
}
