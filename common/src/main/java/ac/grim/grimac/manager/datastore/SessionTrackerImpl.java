package ac.grim.grimac.manager.datastore;

import ac.grim.grimac.api.storage.DataStore;
import ac.grim.grimac.api.storage.category.Categories;
import ac.grim.grimac.api.storage.model.SessionRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/** Keeps connection identity and metadata alive independently of database ownership. */
public final class SessionTrackerImpl implements SessionTracker {
    private final DataStore store;
    private final long heartbeatIntervalMs;
    private final BooleanSupplier writesAllowed;
    private final AtomicReference<Ownership> ownership;
    private final Map<UUID, Connection> connections = new ConcurrentHashMap<>();

    public SessionTrackerImpl(@NotNull DataStore store, @NotNull String serverName, long heartbeatIntervalMs) {
        this(store, serverName, heartbeatIntervalMs, null);
    }

    public SessionTrackerImpl(@NotNull DataStore store, @NotNull String serverName, long heartbeatIntervalMs,
                              @Nullable UUID startupId) {
        this(store, serverName, heartbeatIntervalMs, startupId, () -> true);
    }

    SessionTrackerImpl(DataStore store, String serverName, long heartbeatIntervalMs,
                       @Nullable UUID startupId, BooleanSupplier writesAllowed) {
        this.store = store;
        this.heartbeatIntervalMs = heartbeatIntervalMs;
        this.writesAllowed = writesAllowed;
        this.ownership = new AtomicReference<>(new Ownership(startupId, startupId == null ? Set.of() : Set.of(startupId), Set.of()));
    }

    @Override
    public @NotNull UUID observeActivity(@NotNull UUID player, long now, @NotNull ClientMeta meta) {
        while (true) {
            Connection connection = connections.get(player);
            if (connection == null) {
                // UUID generation and state construction stay outside map bin locks.
                Connection fresh = new Connection(player, now, meta);
                Connection existing = connections.putIfAbsent(player, fresh);
                connection = existing == null ? fresh : existing;
            }
            State current = connection.state.get();
            if (current.closedAt != SessionRecord.OPEN) {
                connections.remove(player, connection);
                continue;
            }
            State next = new State(Math.max(current.lastActivity, now), Math.max(current.lastRequested, now),
                    SessionRecord.OPEN, mergeMeta(current.meta, meta));
            if (connection.state.compareAndSet(current, next)) {
                publishLatest(connection);
                return connection.sessionId;
            }
        }
    }

    @Override
    public void pollHeartbeat(@NotNull UUID player, long now) {
        if (heartbeatIntervalMs <= 0) return;
        Connection connection = connections.get(player);
        if (connection == null) return;
        State current = connection.state.get();
        if (current.closedAt != SessionRecord.OPEN || now - current.lastRequested < heartbeatIntervalMs) return;
        State next = new State(Math.max(current.lastActivity, now), now, SessionRecord.OPEN, current.meta);
        // A competing update already observed activity or closed the connection.
        if (connection.state.compareAndSet(current, next)) publishLatest(connection);
    }

    @Override
    public void close(@NotNull UUID player, long now, @NotNull ClientMeta meta) {
        Connection connection = connections.get(player);
        if (connection == null) return;
        while (true) {
            State current = connection.state.get();
            if (current.closedAt != SessionRecord.OPEN) return;
            State closed = new State(current.lastActivity, current.lastRequested, now, mergeMeta(current.meta, meta));
            if (connection.state.compareAndSet(current, closed)) {
                connections.remove(player, connection);
                publishLatest(connection);
                return;
            }
        }
    }

    @Override
    public @Nullable UUID currentSessionId(@NotNull UUID player) {
        Connection connection = connections.get(player);
        return connection == null || connection.state.get().closedAt != SessionRecord.OPEN ? null : connection.sessionId;
    }

    @FunctionalInterface
    interface SessionResumer {
        void reassign(UUID sessionId, Set<UUID> previousStartups, UUID startupId) throws Exception;
    }

    /** Called with normal writes paused and the replacement lease already claimed. */
    Runnable prepareResume(UUID nextStartup, SessionResumer resumer) throws Exception {
        Set<Connection> active = Set.copyOf(connections.values());
        Ownership next = ownership.updateAndGet(current -> {
            Set<UUID> known = new HashSet<>(current.knownStartups);
            known.add(nextStartup);
            Set<Connection> pending = new HashSet<>(current.pendingConnections);
            pending.addAll(active);
            // Retain before the first handoff. A failed attempt may have moved
            // rows already, and a concurrent quit removes them from the active map.
            return new Ownership(nextStartup, Set.copyOf(known), Set.copyOf(pending));
        });
        for (Connection connection : next.pendingConnections) {
            if (connection.state.get().closedAt == SessionRecord.OPEN) {
                resumer.reassign(connection.sessionId, next.knownStartups, nextStartup);
            }
        }
        return () -> finishResume(next);
    }

    private void finishResume(Ownership expected) {
        if (ownership.get() != expected) return;
        if (!writesAllowed.getAsBoolean()) throw new IllegalStateException("session recovery completion is paused");
        // Include terminal states retained by every failed attempt, as well as
        // joins after the current handoff snapshot. Clear only after completion;
        // an older callback must not discard a newer attempt's retained states.
        Set<Connection> flush = new HashSet<>(expected.pendingConnections);
        flush.addAll(connections.values());
        for (Connection connection : flush) publishLatest(connection);
        if (!writesAllowed.getAsBoolean()) throw new IllegalStateException("session recovery completion was interrupted");
        ownership.compareAndSet(expected, new Ownership(expected.startupId, Set.of(expected.startupId), Set.of()));
    }

    /**
     * One caller submits snapshots for a connection. Other callers only request
     * another pass and return; they never wait for the submitting thread. This
     * keeps a delayed heartbeat from landing after a newer metadata/quit write.
     * The datastore submission itself is a nonblocking ring operation.
     */
    private void publishLatest(Connection connection) {
        if (connection.publications.getAndIncrement() != 0) return;
        int pending = 1;
        try {
            do {
                if (writesAllowed.getAsBoolean()) {
                    store.submit(Categories.SESSION, event -> {
                        State snapshot = connection.state.get();
                        // A producer can be descheduled before claiming its ring
                        // slot. Resolve ownership when that slot is actually filled.
                        event.sessionId(connection.sessionId).playerUuid(connection.player)
                                .startedEpochMs(connection.started).lastActivityEpochMs(snapshot.lastActivity)
                                .closedAtEpochMs(snapshot.closedAt).clientBrand(snapshot.meta.clientBrand())
                                .clientVersion(snapshot.meta.clientVersion()).startupId(ownership.get().startupId);
                    });
                }
                pending = connection.publications.addAndGet(-pending);
            } while (pending != 0);
        } catch (RuntimeException | Error failure) {
            // A failed submission must not leave future publication requests
            // behind a permanently busy publisher. The current state is retained.
            connection.publications.set(0);
            throw failure;
        }
    }

    private static ClientMeta mergeMeta(ClientMeta current, ClientMeta incoming) {
        return new ClientMeta(
                incoming.grimVersion() != null ? incoming.grimVersion() : current.grimVersion(),
                current.clientBrand() != null ? current.clientBrand() : incoming.clientBrand(),
                incoming.clientVersion() >= 0 ? incoming.clientVersion() : current.clientVersion(),
                incoming.serverVersion() != null ? incoming.serverVersion() : current.serverVersion());
    }

    private record Ownership(@Nullable UUID startupId, Set<UUID> knownStartups, Set<Connection> pendingConnections) {}
    private record State(long lastActivity, long lastRequested, long closedAt, ClientMeta meta) {}

    private static final class Connection {
        final UUID player;
        final UUID sessionId = UUID.randomUUID();
        final long started;
        final AtomicReference<State> state;
        final AtomicInteger publications = new AtomicInteger();

        Connection(UUID player, long now, ClientMeta meta) {
            this.player = player;
            this.started = now;
            this.state = new AtomicReference<>(new State(now, now, SessionRecord.OPEN, meta));
        }
    }
}
