package ac.grim.grimac.manager.report;

import ac.grim.grimac.GrimAPI;

import java.util.Objects;
import java.util.UUID;

public final class Report {
    private final String id;
    private final long timestamp;
    private final String reporterName;
    private final String reporterUuid;
    private final String referencedEventId;
    private final String targetPlayer;
    private final String reason;
    private final String grimVersion;
    private final double tps;

    public Report(String id, long timestamp, String reporterName, String reporterUuid, String referencedEventId,
                  String targetPlayer, String reason, String grimVersion, double tps) {
        this.id = Objects.requireNonNull(id);
        this.timestamp = timestamp;
        this.reporterName = reporterName;
        this.reporterUuid = reporterUuid;
        this.referencedEventId = referencedEventId;
        this.targetPlayer = targetPlayer;
        this.reason = reason;
        this.grimVersion = grimVersion;
        this.tps = tps;
    }

    public static Report create(String reporterName, String reporterUuid, String referencedEventId, String targetPlayer, String reason) {
        String id = UUID.randomUUID().toString();
        long ts = System.currentTimeMillis();
        String v = GrimAPI.INSTANCE.getExternalAPI().getGrimVersion();
        double tps = GrimAPI.INSTANCE.getPlatformServer().getTPS();
        return new Report(id, ts, reporterName, reporterUuid, referencedEventId, targetPlayer, reason, v, tps);
    }

    public String getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getReporterName() {
        return reporterName;
    }

    public String getReporterUuid() {
        return reporterUuid;
    }

    public String getReferencedEventId() {
        return referencedEventId;
    }

    public String getTargetPlayer() {
        return targetPlayer;
    }

    public String getReason() {
        return reason;
    }

    public String getGrimVersion() {
        return grimVersion;
    }

    public double getTps() {
        return tps;
    }
}
