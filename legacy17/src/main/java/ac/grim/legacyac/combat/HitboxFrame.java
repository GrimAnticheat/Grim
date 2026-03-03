package ac.grim.legacyac.combat;

public final class HitboxFrame {
    private final long timestampMillis;
    private final boolean teleportMarker;
    private final double minX;
    private final double minY;
    private final double minZ;
    private final double maxX;
    private final double maxY;
    private final double maxZ;

    public HitboxFrame(long timestampMillis, boolean teleportMarker, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.timestampMillis = timestampMillis;
        this.teleportMarker = teleportMarker;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    public boolean isTeleportMarker() {
        return teleportMarker;
    }

    public double getMinX() { return minX; }
    public double getMinY() { return minY; }
    public double getMinZ() { return minZ; }
    public double getMaxX() { return maxX; }
    public double getMaxY() { return maxY; }
    public double getMaxZ() { return maxZ; }
}
