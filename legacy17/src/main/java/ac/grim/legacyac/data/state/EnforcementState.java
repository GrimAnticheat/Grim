package ac.grim.legacyac.data.state;

import ac.grim.legacyac.debug.DetectionEvidence;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Domain state aggregate for enforcement data.
 * Tracks violation levels, buffers, punish state, detection evidence, and debug
 * flags.
 */
public final class EnforcementState {
    private final Map<String, Double> violations = new HashMap<String, Double>();
    private final Map<String, Double> buffers = new HashMap<String, Double>();
    private final Map<String, Boolean> punishExecuted = new HashMap<String, Boolean>();
    private final LinkedList<DetectionEvidence> detectionEvidenceBuffer = new LinkedList<DetectionEvidence>();
    private static final int EVIDENCE_BUFFER_LIMIT = 160;

    private boolean debugEnabled;
    private String detectionSource = "UNKNOWN";
    private int detectionTick;

    // ── Violation management ────────────────────────────────────────────

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

    public double scaleBuffer(String check, double factor) {
        double next = getBuffer(check) * factor;
        if (next <= 0.0001D) {
            buffers.remove(check);
            return 0.0D;
        }
        buffers.put(check, next);
        return next;
    }

    public void decayViolations(double amount) {
        for (Iterator<Map.Entry<String, Double>> it = violations.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Double> entry = it.next();
            double next = entry.getValue().doubleValue() - amount;
            if (next <= 0.0D) {
                it.remove();
            } else {
                entry.setValue(next);
            }
        }
        for (Iterator<Map.Entry<String, Double>> it = buffers.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Double> entry = it.next();
            double next = entry.getValue().doubleValue() - (amount * 0.5D);
            if (next <= 0.0D) {
                it.remove();
            } else {
                entry.setValue(next);
            }
        }
    }

    // ── Punish tracking ─────────────────────────────────────────────────

    public boolean hasExecutedPunish(String check) {
        Boolean value = punishExecuted.get(check);
        return value != null && value.booleanValue();
    }

    public void markPunishExecuted(String check) {
        punishExecuted.put(check, Boolean.TRUE);
    }

    // ── Evidence ────────────────────────────────────────────────────────

    public void recordDetectionEvidence(DetectionEvidence evidence) {
        if (evidence == null)
            return;
        detectionEvidenceBuffer.addLast(evidence);
        while (detectionEvidenceBuffer.size() > EVIDENCE_BUFFER_LIMIT) {
            detectionEvidenceBuffer.removeFirst();
        }
    }

    public List<DetectionEvidence> getDetectionEvidenceSnapshot() {
        return Collections.unmodifiableList(new ArrayList<DetectionEvidence>(detectionEvidenceBuffer));
    }

    public double getDetectionOffsetP95() {
        if (detectionEvidenceBuffer.isEmpty())
            return 0.0D;
        List<Double> offsets = new ArrayList<Double>();
        for (DetectionEvidence evidence : detectionEvidenceBuffer) {
            offsets.add(Double.valueOf(Math.max(0.0D, evidence.getOffset())));
        }
        Collections.sort(offsets);
        int index = (int) Math.ceil(offsets.size() * 0.95D) - 1;
        if (index < 0)
            index = 0;
        if (index >= offsets.size())
            index = offsets.size() - 1;
        return offsets.get(index).doubleValue();
    }

    public String getRecentTriggerChain(int limit) {
        if (detectionEvidenceBuffer.isEmpty())
            return "none";
        StringBuilder builder = new StringBuilder();
        int start = Math.max(0, detectionEvidenceBuffer.size() - Math.max(1, limit));
        for (int i = start; i < detectionEvidenceBuffer.size(); i++) {
            DetectionEvidence evidence = detectionEvidenceBuffer.get(i);
            if (builder.length() > 0)
                builder.append(" -> ");
            builder.append(evidence.getCheck());
            builder.append('@');
            builder.append(evidence.getTick());
        }
        return builder.toString();
    }

    // ── Debug / detection context ───────────────────────────────────────

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

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }
}
