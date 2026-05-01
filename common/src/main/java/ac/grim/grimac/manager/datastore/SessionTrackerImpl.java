package ac.grim.grimac.manager.datastore;

import ac.grim.grimac.api.storage.DataStore;
import ac.grim.grimac.api.storage.category.Categories;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concrete {@link SessionTracker}. The disabled-datastore path uses
 * {@link SessionTracker#NOOP} instead.
 */
public final class SessionTrackerImpl implements SessionTracker {

    private final DataStore store;
    private final String serverName;
    private final long heartbeatIntervalMs;
    private final Map<UUID, State> states = new ConcurrentHashMap<>();

    public SessionTrackerImpl(@NotNull DataStore store, @NotNull String serverName, long heartbeatIntervalMs) {
        this.store = store;
        this.serverName = serverName;
        this.heartbeatIntervalMs = heartbeatIntervalMs;
    }

    @Override
    public @NotNull UUID observeActivity(
            @NotNull UUID playerUuid,
            long now,
            @NotNull ClientMeta meta) {
        // Lock-free CAS retry loop. get/putIfAbsent/replace hold a bin lock only for the CAS itself — no user code under the lock. UUID.randomUUID() (~1µs) runs unlocked. The loop keeps a fresh in-memory session_id on race-with-close (unconditional put would have re-inserted the closed session's id and a quick reconnect would inherit it).
        UUID candidateSessionId = null;
        while (true) {
            State current = states.get(playerUuid);
            if (current == null) {
                if (candidateSessionId == null) candidateSessionId = UUID.randomUUID();
                State fresh = new State(candidateSessionId, now, now, now, meta);
                if (states.putIfAbsent(playerUuid, fresh) == null) {
                    emit(fresh, playerUuid, now, null); // closed_at stays null while alive
                    return fresh.sessionId;
                }
                // Lost the insert race — someone inserted between get and putIfAbsent. Retry as update.
                continue;
            }
            State next = new State(current.sessionId, current.startedEpochMs, now, now, meta);
            if (states.replace(playerUuid, current, next)) {
                emit(next, playerUuid, now, null);
                return next.sessionId;
            }
            // CAS lost — state changed (close removed it, or another observe replaced it). Retry.
        }
    }

    @Override
    public void pollHeartbeat(@NotNull UUID playerUuid, long now) {
        if (heartbeatIntervalMs <= 0) return;
        State current = states.get(playerUuid);
        if (current == null) return;
        if (now - current.lastEmittedEpochMs < heartbeatIntervalMs) return;
        State next = new State(current.sessionId, current.startedEpochMs, now, now, current.cachedMeta);
        // CAS the new state in. If another thread (rare — pollData runs on a
        // single tick scheduler per player) beat us, just skip — they'll emit.
        if (states.replace(playerUuid, current, next)) {
            emit(next, playerUuid, now, null);
        }
    }

    @Override
    public void close(@NotNull UUID playerUuid, long now, @NotNull ClientMeta meta) {
        State prev = states.remove(playerUuid);
        if (prev == null) return;
        emit(new State(prev.sessionId, prev.startedEpochMs, now, now, meta), playerUuid, now, now);
    }

    @Override
    public @Nullable UUID currentSessionId(@NotNull UUID playerUuid) {
        State s = states.get(playerUuid);
        return s == null ? null : s.sessionId;
    }

    private void emit(State s, UUID playerUuid, long now, @Nullable Long closedAt) {
        final UUID sessionId = s.sessionId;
        final long started = s.startedEpochMs;
        final ClientMeta meta = s.cachedMeta;
        store.submit(Categories.SESSION, e -> e
                .sessionId(sessionId)
                .playerUuid(playerUuid)
                .serverName(serverName)
                .startedEpochMs(started)
                .lastActivityEpochMs(now)
                .closedAtEpochMs(closedAt)
                .grimVersion(meta.grimVersion())
                .clientBrand(meta.clientBrand())
                .clientVersion(meta.clientVersion())
                .serverVersionString(meta.serverVersion()));
    }

    private record State(UUID sessionId,
                         long startedEpochMs,
                         long lastActivityEpochMs,
                         long lastEmittedEpochMs,
                         ClientMeta cachedMeta) {}
}
