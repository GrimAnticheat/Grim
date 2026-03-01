package ac.grim.legacyac.network.frame;

public final class MovementFrame {
    public enum Source {
        PACKET_FLYING,
        PACKET_POSITION,
        PACKET_LOOK,
        PACKET_POSITION_LOOK,
        BUKKIT_MOVE_EVENT
    }

    private final long timestampNanos;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    private final boolean onGround;
    private final Source source;

    public MovementFrame(long timestampNanos, double x, double y, double z, float yaw, float pitch, boolean onGround, Source source) {
        this.timestampNanos = timestampNanos;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
        this.source = source;
    }

    public long getTimestampNanos() {
        return timestampNanos;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public Source getSource() {
        return source;
    }
}
