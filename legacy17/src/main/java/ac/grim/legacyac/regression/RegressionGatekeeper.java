package ac.grim.legacyac.regression;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Phase E: Release gatekeeper that evaluates regression criteria.
 *
 * <p>
 * The gatekeeper is parameterized with thresholds (from config) and
 * produces a pass/fail verdict with explanations. If ANY gate fails,
 * the new pipeline should NOT be made the default path.
 * </p>
 *
 * <p>
 * PRD §8-E: "设定上线门槛：未达标则不切换默认路径"
 * </p>
 */
public final class RegressionGatekeeper {

    /** Maximum acceptable false-positive rate per check (default 10%). */
    private final double maxFPRatePerCheck;

    /** Maximum acceptable overall false-positive rate (default 5%). */
    private final double maxOverallFPRate;

    /** Maximum acceptable trigger latency in ms (default 200ms). */
    private final long maxTriggerLatencyMs;

    /**
     * Minimum required flag count for a check to be evaluated (too few =
     * inconclusive).
     */
    private final long minFlagsForEvaluation;

    public RegressionGatekeeper(double maxFPRatePerCheck, double maxOverallFPRate,
            long maxTriggerLatencyMs, long minFlagsForEvaluation) {
        this.maxFPRatePerCheck = maxFPRatePerCheck;
        this.maxOverallFPRate = maxOverallFPRate;
        this.maxTriggerLatencyMs = maxTriggerLatencyMs;
        this.minFlagsForEvaluation = minFlagsForEvaluation;
    }

    /** Convenience constructor with sensible defaults. */
    public RegressionGatekeeper() {
        this(0.10D, 0.05D, 200L, 10L);
    }

    /**
     * Evaluate the regression report against the thresholds.
     *
     * @param report the report to evaluate
     * @return a GateResult with pass/fail and reasons
     */
    public GateResult evaluate(RegressionReport report) {
        List<String> failures = new ArrayList<String>();
        List<String> warnings = new ArrayList<String>();

        // ── Gate 1: Overall FP rate ─────────────────────────────────────
        double overallFP = report.getTotalFlags() > 0
                ? (double) report.getTotalExempted() / report.getTotalFlags()
                : 0.0D;
        if (report.getTotalFlags() >= minFlagsForEvaluation && overallFP > maxOverallFPRate) {
            failures.add(String.format(Locale.ROOT,
                    "Overall FP rate %.1f%% exceeds threshold %.1f%%",
                    overallFP * 100.0D, maxOverallFPRate * 100.0D));
        }

        // ── Gate 2: Per-check FP rate ───────────────────────────────────
        for (Map.Entry<String, ViolationLedger.CheckStats> entry : report.getCheckStats().entrySet()) {
            ViolationLedger.CheckStats cs = entry.getValue();
            if (cs.getTotalFlags() < minFlagsForEvaluation) {
                warnings.add(entry.getKey() + ": insufficient data (" + cs.getTotalFlags() + " flags)");
                continue;
            }
            double fpRate = cs.estimatedFalsePositiveRate();
            if (fpRate > maxFPRatePerCheck) {
                failures.add(String.format(Locale.ROOT,
                        "%s FP rate %.1f%% exceeds per-check threshold %.1f%%",
                        entry.getKey(), fpRate * 100.0D, maxFPRatePerCheck * 100.0D));
            }
        }

        // ── Gate 3: Trigger latency ─────────────────────────────────────
        for (Map.Entry<String, ViolationLedger.CheckStats> entry : report.getCheckStats().entrySet()) {
            ViolationLedger.CheckStats cs = entry.getValue();
            if (cs.getMaxTriggerLatencyMs() > maxTriggerLatencyMs) {
                failures.add(String.format(Locale.ROOT,
                        "%s max trigger latency %dms exceeds threshold %dms",
                        entry.getKey(), cs.getMaxTriggerLatencyMs(), maxTriggerLatencyMs));
            }
        }

        // ── Gate 4: Sanity — at least one check must have data ──────────
        if (report.getTotalFlags() == 0 && report.getCheckStats().isEmpty()) {
            warnings.add("No violation data collected — cannot evaluate regression");
        }

        boolean passed = failures.isEmpty();
        return new GateResult(passed, failures, warnings);
    }

    // ── Result ──────────────────────────────────────────────────────────

    public static final class GateResult {
        private final boolean passed;
        private final List<String> failures;
        private final List<String> warnings;

        GateResult(boolean passed, List<String> failures, List<String> warnings) {
            this.passed = passed;
            this.failures = failures;
            this.warnings = warnings;
        }

        public boolean isPassed() {
            return passed;
        }

        public List<String> getFailures() {
            return failures;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public String toReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("══════════════ GATE RESULT: ").append(passed ? "✅ PASS" : "❌ FAIL").append(" ══════════════\n");
            if (!failures.isEmpty()) {
                sb.append("FAILURES:\n");
                for (String f : failures) {
                    sb.append("  ✗ ").append(f).append('\n');
                }
            }
            if (!warnings.isEmpty()) {
                sb.append("WARNINGS:\n");
                for (String w : warnings) {
                    sb.append("  ⚠ ").append(w).append('\n');
                }
            }
            if (passed && failures.isEmpty() && warnings.isEmpty()) {
                sb.append("  All gates passed with no warnings.\n");
            }
            sb.append("══════════════════════════════════════════════════════");
            return sb.toString();
        }

        public String toSummary() {
            return String.format(Locale.ROOT, "[GATE] %s failures=%d warnings=%d",
                    passed ? "PASS" : "FAIL", failures.size(), warnings.size());
        }
    }
}
