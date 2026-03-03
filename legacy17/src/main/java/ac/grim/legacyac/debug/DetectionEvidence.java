package ac.grim.legacyac.debug;

public final class DetectionEvidence {
    private final long timestampMillis;
    private final String check;
    private final double offset;
    private final double buffer;
    private final double vl;
    private final double rtt;
    private final String source;
    private final int tick;

    public DetectionEvidence(long timestampMillis, String check, double offset, double buffer, double vl, double rtt, String source, int tick) {
        this.timestampMillis = timestampMillis;
        this.check = check;
        this.offset = offset;
        this.buffer = buffer;
        this.vl = vl;
        this.rtt = rtt;
        this.source = source;
        this.tick = tick;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    public String getCheck() {
        return check;
    }

    public double getOffset() {
        return offset;
    }

    public double getBuffer() {
        return buffer;
    }

    public double getVl() {
        return vl;
    }

    public double getRtt() {
        return rtt;
    }

    public String getSource() {
        return source;
    }

    public int getTick() {
        return tick;
    }
}
