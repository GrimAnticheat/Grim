package ac.grim.legacyac.regression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Phase E: Immutable regression report snapshot.
 *
 * <p>
 * Produced by {@link ViolationLedger#generateReport()}, this contains
 * aggregate statistics and pass/fail verdicts suitable for admin review
 * and programmatic release-gating.
 * </p>
 */
public final class RegressionReport {
    private final Map<String, ViolationLedger.CheckStats> checkStats;
    private final long totalFlags;
    private final long totalExempted;
    private final String worstCheck;
    private final double worstFPRate;

    RegressionReport(Map<String, ViolationLedger.CheckStats> checkStats, long totalFlags, long totalExempted,
            String worstCheck, double worstFPRate) {
        this.checkStats = checkStats;
        this.totalFlags = totalFlags;
        this.totalExempted = totalExempted;
        this.worstCheck = worstCheck;
        this.worstFPRate = worstFPRate;
    }

    // ── Read APIs ───────────────────────────────────────────────────────

    public Map<String, ViolationLedger.CheckStats> getCheckStats() {
        return checkStats;
    }

    public long getTotalFlags() {
        return totalFlags;
    }

    public long getTotalExempted() {
        return totalExempted;
    }

    public String getWorstCheck() {
        return worstCheck;
    }

    public double getWorstFPRate() {
        return worstFPRate;
    }

    // ── Report output ───────────────────────────────────────────────────

    /**
     * Compact multi-line report.
     */
    public String toReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════ REGRESSION REPORT ═══════════════════\n");
        sb.append(String.format(Locale.ROOT,
                "Total Flags: %d | Exempted: %d | Overall FP≈%.1f%%\n",
                totalFlags, totalExempted,
                totalFlags > 0 ? (totalExempted * 100.0D / totalFlags) : 0.0D));
        sb.append(String.format(Locale.ROOT, "Worst Check: %s (FP≈%.1f%%)\n", worstCheck, worstFPRate * 100.0D));
        sb.append("─────────────────── Per-Check Detail ───────────────────\n");

        List<String> sortedNames = new ArrayList<String>(checkStats.keySet());
        Collections.sort(sortedNames);
        for (String name : sortedNames) {
            ViolationLedger.CheckStats cs = checkStats.get(name);
            if (cs != null) {
                sb.append("  ").append(cs.toString()).append('\n');
            }
        }
        sb.append("════════════════════════════════════════════════════════");
        return sb.toString();
    }

    /**
     * Admin-friendly single-line summary.
     */
    public String toSummary() {
        return String.format(Locale.ROOT,
                "[REGRESSION] flags=%d exempted=%d overallFP=%.1f%% worst=%s(%.1f%%)",
                totalFlags, totalExempted,
                totalFlags > 0 ? (totalExempted * 100.0D / totalFlags) : 0.0D,
                worstCheck, worstFPRate * 100.0D);
    }
}
