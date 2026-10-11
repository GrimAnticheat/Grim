package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Optional block velocity profiles applied before movement collision prediction. */
public final class ClimbingVelocity {
    private static final Profile VANILLA = new Profile(0.2, -0.15F);
    private static final ConcurrentMap<StateType, Profile> PROFILES = new ConcurrentHashMap<>();

    private ClimbingVelocity() {
    }

    public interface Registration extends AutoCloseable {
        @Override
        void close();
    }

    /** Register exact block physics and close the registration when its owner disables. */
    public static Registration register(StateType type, double ascentSpeed, double descentMinimum) {
        Objects.requireNonNull(type, "type");
        if (!Double.isFinite(ascentSpeed) || ascentSpeed < 0
                || !Double.isFinite(descentMinimum) || descentMinimum > 0) {
            throw new IllegalArgumentException("Climbing velocities must be finite, with nonnegative ascent and nonpositive descent");
        }
        Profile profile = new Profile(ascentSpeed, descentMinimum);
        if (PROFILES.putIfAbsent(type, profile) != null) {
            throw new IllegalStateException("A climbing velocity profile is already registered for this block type");
        }
        AtomicBoolean closed = new AtomicBoolean();
        return () -> {
            if (closed.compareAndSet(false, true)) {
                PROFILES.remove(type, profile);
            }
        };
    }

    public static double ascentSpeed(GrimPlayer player, double x, double y, double z) {
        if ((player.wasTouchingWater || player.wasTouchingLava) && !player.isFlying) {
            return VANILLA.ascentSpeed;
        }
        return profile(player, x, y, z).ascentSpeed;
    }

    public static double descentMinimum(GrimPlayer player, double x, double y, double z) {
        return profile(player, x, y, z).descentMinimum;
    }

    private static Profile profile(GrimPlayer player, double x, double y, double z) {
        return PROFILES.getOrDefault(player.compensatedWorld.getBlockType(x, y, z), VANILLA);
    }

    private record Profile(double ascentSpeed, double descentMinimum) {
    }
}
