package ac.grim.grimac.utils.data;

import com.github.retrooper.packetevents.util.Vector3d;
import lombok.Getter;
import lombok.Setter;

@Getter
public final class TrackedPosition {

    private static final double MODERN_COORDINATE_SCALE = 4096.0;
    private static final double LEGACY_COORDINATE_SCALE = 32.0;

    private final double scale;
    private double x;
    private double y;
    private double z;

    public TrackedPosition() {
//        this.scale = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) ? MODERN_COORDINATE_SCALE : LEGACY_COORDINATE_SCALE;
        this.scale = MODERN_COORDINATE_SCALE;
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

    // Methods since 1.16.
    public double withDeltaX(long x) {
        if (x == 0L) return this.x;
        return unpack(pack(this.x, scale) + x);
    }

    public double withDeltaY(long y) {
        if (y == 0L) return this.y;
        return unpack(pack(this.y, scale) + y);
    }

    public double withDeltaZ(long z) {
        if (z == 0L) return this.z;
        return unpack(pack(this.z, scale) + z);
    }

    // In 1.16-, these were different.
    public double withDeltaLegacyX(double x) {
        return unpackLegacy(packLegacy(this.x, scale) + x);
    }

    public double withDeltaLegacyY(double y) {
        return unpackLegacy(packLegacy(this.y, scale) + y);
    }

    public double withDeltaLegacyZ(double z) {
        return unpackLegacy(packLegacy(this.z, scale) + z);
    }

    public void setPos(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
