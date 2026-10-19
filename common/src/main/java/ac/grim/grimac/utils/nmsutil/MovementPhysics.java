package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.UUID;
import java.util.Objects;

/** Server-owned client prediction profiles; movement and validation remain enabled. */
public final class MovementPhysics {
    private static final ConcurrentMap<UUID, RegisteredProfile> PROFILES = new ConcurrentHashMap<>();

    private MovementPhysics() {}

    public interface Registration extends AutoCloseable {
        @Override void close();
    }

    /** Register the actual physics of a supported client and close when its owner disables. */
    public static Registration register(UUID playerId, int protocol, double componentThreshold, boolean preserveUpwardJump) {
        return register(playerId, protocol, componentThreshold, preserveUpwardJump, false);
    }

    /** Select float multiplication for the sprint jump impulse when implemented by this client. */
    public static Registration register(UUID playerId, int protocol, double componentThreshold, boolean preserveUpwardJump, boolean floatSprintJump) {
        return register(playerId, protocol, componentThreshold, preserveUpwardJump, floatSprintJump, 0);
    }

    /** Select the additional fishing pull lift implemented by this supported client. */
    public static Registration register(UUID playerId, int protocol, double componentThreshold, boolean preserveUpwardJump, boolean floatSprintJump, double fishingPullLiftCoefficient) {
        return register(playerId, protocol, componentThreshold, preserveUpwardJump, floatSprintJump, fishingPullLiftCoefficient, true);
    }

    /** Select off-hand equipment eligibility implemented by this supported client. */
    public static Registration register(UUID playerId, int protocol, double componentThreshold, boolean preserveUpwardJump, boolean floatSprintJump, double fishingPullLiftCoefficient, boolean offHandEnabled) {
        Objects.requireNonNull(playerId, "playerId");
        if (protocol < 0 || !Double.isFinite(componentThreshold) || componentThreshold < 0
                || !Double.isFinite(fishingPullLiftCoefficient) || fishingPullLiftCoefficient < 0) {
            throw new IllegalArgumentException("Protocol and finite movement threshold must be nonnegative");
        }
        Profile profile = new Profile(componentThreshold, preserveUpwardJump, floatSprintJump, fishingPullLiftCoefficient, offHandEnabled);
        RegisteredProfile entry = new RegisteredProfile(protocol, profile);
        if (PROFILES.putIfAbsent(playerId, entry) != null) {
            throw new IllegalStateException("A movement profile is already registered for this player");
        }
        AtomicBoolean closed = new AtomicBoolean();
        return () -> {
            if (closed.compareAndSet(false, true)) PROFILES.remove(playerId, entry);
        };
    }

    /** Vehicles retain their own native prediction engines and physics. */
    public static Profile profile(GrimPlayer player) {
        return player.inVehicle() ? null : registeredProfile(player);
    }

    /** Equipment eligibility applies while riding too; unregistered clients keep native behavior. */
    public static boolean offHandEnabled(GrimPlayer player) {
        Profile profile = registeredProfile(player);
        return profile == null || profile.offHandEnabled;
    }

    private static Profile registeredProfile(GrimPlayer player) {
        if (PROFILES.isEmpty()) return null;
        RegisteredProfile entry = PROFILES.get(player.uuid);
        return entry != null && entry.protocol == player.getClientVersion().getProtocolVersion() ? entry.profile : null;
    }

    public static double componentThreshold(GrimPlayer player, double nativeThreshold) {
        Profile profile = profile(player);
        return profile == null ? nativeThreshold : profile.componentThreshold;
    }

    public record Profile(double componentThreshold, boolean preserveUpwardJump, boolean floatSprintJump, double fishingPullLiftCoefficient, boolean offHandEnabled) {
        public Profile(double componentThreshold, boolean preserveUpwardJump, boolean floatSprintJump, double fishingPullLiftCoefficient) {
            this(componentThreshold, preserveUpwardJump, floatSprintJump, fishingPullLiftCoefficient, true);
        }
        public Profile(double componentThreshold, boolean preserveUpwardJump) {
            this(componentThreshold, preserveUpwardJump, false, 0);
        }
        public Profile(double componentThreshold, boolean preserveUpwardJump, boolean floatSprintJump) {
            this(componentThreshold, preserveUpwardJump, floatSprintJump, 0);
        }
    }
    private record RegisteredProfile(int protocol, Profile profile) {}
}
