package ac.grim.grimac.manager.datastore;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Tracks a current session id per connected player. A session is bounded by
 * the player's connection lifetime: opens on the first activity after join,
 * closes when {@link #close} is called from the disconnect path. Each
 * observed activity publishes a {@code SessionEvent} upsert with the latest
 * {@code lastActivityEpochMs}, so the backend always has a recent heartbeat
 * for {@code /grim history}.
 *
 * <p>{@link #pollHeartbeat} is the periodic-bump entry point — called per
 * tick from {@code GrimPlayer.pollData}, throttled internally to emit at
 * most once per heartbeat interval.
 *
 * <p>Datastore-disabled or init-failed paths get {@link #NOOP}, which
 * implements every method as a no-op so callers don't null-check.
 */
public interface SessionTracker {

    /**
     * Ensure the player has an open session and extend its activity to {@code now}.
     * Returns the current sessionId — callers use it as the {@code sessionId} field
     * on any downstream events submitted in the same logical activity tick.
     */
    @NotNull UUID observeActivity(@NotNull UUID playerUuid, long now, @NotNull ClientMeta meta);

    /**
     * Periodic heartbeat from {@code pollData}. Throttles internally; no-op
     * when the player has no active session or the heartbeat interval is 0.
     */
    void pollHeartbeat(@NotNull UUID playerUuid, long now);

    /**
     * Final activity heartbeat for a disconnecting player + drop the in-memory
     * state so the next join opens a fresh session. Stamps {@code closed_at}
     * on the row so the renderer can distinguish a graceful end from a crash.
     */
    void close(@NotNull UUID playerUuid, long now, @NotNull ClientMeta meta);

    /**
     * Returns the current session id for a player, or {@code null} when this
     * tracker has never observed them.
     */
    @Nullable UUID currentSessionId(@NotNull UUID playerUuid);

    /**
     * Optional client/server metadata stamped on every session upsert.
     * {@code clientVersion} is a PacketEvents protocol-version number; {@code -1}
     * means unknown. Use {@link #empty()} when none of the fields are known.
     */
    record ClientMeta(
            String grimVersion,
            String clientBrand,
            int clientVersion,
            String serverVersion) {

        public static ClientMeta empty() {
            return new ClientMeta(null, null, -1, null);
        }
    }

    /**
     * No-op tracker returned by {@code DataStoreLifecycle.sessionTracker()}
     * when the v1 datastore is disabled or its init failed. Returns a fresh
     * UUID from {@link #observeActivity} so the contract holds; nothing is
     * persisted.
     */
    SessionTracker NOOP = new SessionTracker() {
        @Override public @NotNull UUID observeActivity(@NotNull UUID p, long n, @NotNull ClientMeta m) { return UUID.randomUUID(); }
        @Override public void pollHeartbeat(@NotNull UUID p, long n) {}
        @Override public void close(@NotNull UUID p, long n, @NotNull ClientMeta m) {}
        @Override public @Nullable UUID currentSessionId(@NotNull UUID p) { return null; }
    };
}
