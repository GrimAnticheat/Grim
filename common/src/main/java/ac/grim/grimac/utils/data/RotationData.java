package ac.grim.grimac.utils.data;

import org.jetbrains.annotations.Contract;

public record RotationData(
        float yaw, float pitch,
        boolean relativeYaw, boolean relativePitch,
        int transaction) {

    @Contract(pure = true)
    public boolean allowRotation(float yaw, float pitch) {
        // TODO: pitch bounds?
        return (this.relativeYaw || this.yaw == yaw) && (this.relativePitch || this.pitch == pitch);
    }
}
