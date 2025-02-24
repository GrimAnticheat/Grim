package ac.grim.grimac.utils.data;

import com.github.retrooper.packetevents.util.Vector3d;

public final class TrackedPosition {

    private static final double MODERN_COORDINATE_SCALE = 4096.0;
    private static final double LEGACY_COORDINATE_SCALE = 32.0;

    private final double scale;
    private Vector3d pos = new Vector3d();

    public TrackedPosition() {
//        this.scale = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) ? MODERN_COORDINATE_SCALE : LEGACY_COORDINATE_SCALE;
        this.scale = MODERN_COORDINATE_SCALE;
    }

    public double getScale() {
        return scale;
    }

    public static long pack(double value, double scale) {
        return Math.round(value * scale);
    }

    public static double packLegacy(double value, double scale) {
        return Math.floor(value * scale);
    }

    private double unpack(long value) {
        return (double) value / scale;
    }

    private double unpackLegacy(double value) {
        return value / scale;
    }

    public Vector3d getPos() {
        return pos;
    }

    // Method since 1.16.
    public Vector3d withDelta(long x, long y, long z) {
        if (x == 0L && y == 0L && z == 0L) {
            return this.pos;
        }

        double d = x == 0L ? this.pos.x : unpack(pack(this.pos.x, scale) + x);
        double e = y == 0L ? this.pos.y : unpack(pack(this.pos.y, scale) + y);
        double f = z == 0L ? this.pos.z : unpack(pack(this.pos.z, scale) + z);
        return new Vector3d(d, e, f);
    }

    // In 1.16-, this was different.
    public Vector3d withDeltaLegacy(double x, double y, double z) {
        double d = unpackLegacy(packLegacy(this.pos.x, scale) + x);
        double e = unpackLegacy(packLegacy(this.pos.y, scale) + y);
        double f = unpackLegacy(packLegacy(this.pos.z, scale) + z);
        return new Vector3d(d, e, f);
    }

    public void setPos(Vector3d pos) {
        this.pos = pos;
    }
}
