package ac.grim.grimac.utils.data;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.util.Vector3d;
import lombok.Getter;
import lombok.Setter;

@Getter
public final class TrackedPosition {

    private static final double MODERN_COORDINATE_SCALE = 4096.0;
    private static final double LEGACY_COORDINATE_SCALE = 32.0;

    private final double scale;
    @Setter
    private Vector3d pos = new Vector3d();

    public TrackedPosition(final GrimPlayer player) {
        this.scale = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) ? MODERN_COORDINATE_SCALE : LEGACY_COORDINATE_SCALE;
    }

    public static long pack(final double value, final double scale) {
        return Math.round(value * scale);
    }

    public static double packLegacy(final double value, final double scale) {
        return Math.floor(value * scale);
    }

    private double unpack(final long value) {
        return (double) value / scale;
    }

    private double unpackLegacy(final double value) {
        return value / scale;
    }

    // Method since 1.16.
    public Vector3d withDelta(final long x, final long y, final long z) {
        if (x == 0L && y == 0L && z == 0L) {
            return this.pos;
        }

        final double d = x == 0L ? this.pos.x : unpack(pack(this.pos.x, scale) + x);
        final double e = y == 0L ? this.pos.y : unpack(pack(this.pos.y, scale) + y);
        final double f = z == 0L ? this.pos.z : unpack(pack(this.pos.z, scale) + z);
        return new Vector3d(d, e, f);
    }

    // In 1.16-, this was different.
    public Vector3d withDeltaLegacy(final double x, final double y, final double z) {
        final double d = unpackLegacy(packLegacy(this.pos.x, scale) + x);
        final double e = unpackLegacy(packLegacy(this.pos.y, scale) + y);
        final double f = unpackLegacy(packLegacy(this.pos.z, scale) + z);
        return new Vector3d(d, e, f);
    }

}