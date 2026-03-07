package ac.grim.legacyac.check.impl.aim;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.frame.MovementFrame;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

public final class AimProcessorCheck extends Check {
    private final Map<UUID, RotationProfile> profiles = new ConcurrentHashMap<UUID, RotationProfile>();

    public AimProcessorCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "AimProcessor");
    }

    public void onMovementFrame(Player player, MovementFrame frame, PlayerData data) {
        if (!frame.hasLook()) {
            return;
        }
        RotationProfile profile = getProfile(player);
        float yawDelta = Math.abs(data.getLastYawDelta());
        float pitchDelta = Math.abs(data.getLastPitchDelta());
        if (yawDelta > 0.0F && yawDelta < 5.0F && profile.lastYawDelta > 0.0F) {
            profile.stepYaw = gcd(profile.lastYawDelta, yawDelta);
        }
        if (pitchDelta > 0.0F && pitchDelta < 5.0F && profile.lastPitchDelta > 0.0F) {
            profile.stepPitch = gcd(profile.lastPitchDelta, pitchDelta);
        }
        profile.lastYawDelta = yawDelta;
        profile.lastPitchDelta = pitchDelta;
    }

    public double getDeltaDotsYaw(Player player, float yawDelta) {
        RotationProfile profile = profiles.get(player.getUniqueId());
        if (profile == null || profile.stepYaw <= 1.0E-6D) {
            return 0.0D;
        }
        return yawDelta / profile.stepYaw;
    }

    private RotationProfile getProfile(Player player) {
        RotationProfile profile = profiles.get(player.getUniqueId());
        if (profile == null) {
            profile = new RotationProfile();
            RotationProfile existing = profiles.putIfAbsent(player.getUniqueId(), profile);
            if (existing != null) {
                profile = existing;
            }
        }
        return profile;
    }

    private static double gcd(double left, double right) {
        double a = Math.abs(left);
        double b = Math.abs(right);
        while (b > 1.0E-4D) {
            double temp = a % b;
            a = b;
            b = temp;
        }
        return a;
    }

    private static final class RotationProfile {
        private float lastYawDelta;
        private float lastPitchDelta;
        private double stepYaw;
        private double stepPitch;
    }
}
