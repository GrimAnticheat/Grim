package ac.grim.grimac.utils.data;

import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.jetbrains.annotations.Contract;

public record RotationData(
        float yaw, float pitch,
        boolean relativeYaw, boolean relativePitch,
        int transaction) {

    public static RotationData fromServer(ClientVersion version, float yaw, float pitch, boolean relativeYaw, boolean relativePitch, int transaction) {
        if (!relativePitch) {
            // since 1.21.9, PLAYER_ROTATION clamps in calculateAbsolute before setXRot
            if (version.isOlderThan(ClientVersion.V_1_21_9)) pitch %= 360.0F;
            pitch = GrimMath.clamp(pitch, -90.0F, 90.0F);
        }

        return new RotationData(yaw, pitch, relativeYaw, relativePitch, transaction);
    }

    @Contract(pure = true)
    public boolean allowRotation(float yaw, float pitch) {
        // TODO: pitch bounds?
        return (this.relativeYaw || this.yaw == yaw) && (this.relativePitch || this.pitch == pitch);
    }
}
