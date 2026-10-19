package ac.grim.grimac.manager.datastore;

import ac.grim.grimac.api.storage.DataStore;
import ac.grim.grimac.api.storage.category.Categories;
import ac.grim.grimac.api.storage.model.SessionRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

/** Keeps connection identity and metadata alive independently of database ownership. */
public final class SessionTrackerImpl implements SessionTracker {
    private final DataStore store;
    private final long heartbeatIntervalMs;
    private final BooleanSupplier writesAllowed;
    private volatile @Nullable UUID startupId;
    private final Set<UUID> knownStartups = new HashSet<>();
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
        this.startupId = startupId;
        this.writesAllowed = writesAllowed;
        if (startupId != null) knownStartups.add(startupId);
    }

    @Override
    public @NotNull UUID observeActivity(@NotNull UUID player, long now, @NotNull ClientMeta meta) {
        while (true) {
            Connection connection = connections.computeIfAbsent(player, id -> new Connection(id, now, meta));
            synchronized (connection) {
                if (connection.closedAt != SessionRecord.OPEN) {
                    connections.remove(player, connection);
                    continue;
                }
                connection.lastActivity = Math.max(connection.lastActivity, now);
                connection.meta = mergeMeta(connection.meta, meta);
                emit(connection);
                return connection.sessionId;
            }
        }
    }

    @Override
    public void pollHeartbeat(@NotNull UUID player, long now) {
        Connection connection = connections.get(player);
        if (connection == null || heartbeatIntervalMs <= 0) return;
        synchronized (connection) {
            if (connection.closedAt != SessionRecord.OPEN) return;
            connection.lastActivity = Math.max(connection.lastActivity, now);
            if (now - connection.lastEmitted >= heartbeatIntervalMs) emit(connection);
        }
    }

    @Override
    public void close(@NotNull UUID player, long now, @NotNull ClientMeta meta) {
        Connection connection = connections.remove(player);
        if (connection == null) return;
        synchronized (connection) {
            connection.closedAt = now;
            connection.meta = mergeMeta(connection.meta, meta);
            emit(connection);
        }
    }

    @Override
    public @Nullable UUID currentSessionId(@NotNull UUID player) {
        Connection connection = connections.get(player);
        if (connection == null) return null;
        synchronized (connection) {
            return connection.closedAt == SessionRecord.OPEN ? connection.sessionId : null;
        }
    }

    @FunctionalInterface
    interface SessionResumer {
        void reassign(UUID sessionId, Set<UUID> previousStartups, UUID startupId) throws Exception;
    }

    /** Called with normal writes paused and the replacement lease already claimed. */
    synchronized Runnable prepareResume(UUID nextStartup, SessionResumer resumer) throws Exception {
        knownStartups.add(nextStartup);
        startupId = nextStartup;
        Set<UUID> previous = Set.copyOf(knownStartups);
        List<Connection> captured = new ArrayList<>(connections.values());
        for (Connection connection : captured) {
            synchronized (connection) {
                if (connection.closedAt != SessionRecord.OPEN) continue;
            }
            // Never hold a connection lock during database I/O. Quit/brand/join
            // events keep updating memory while this call is blocked.
            resumer.reassign(connection.sessionId, previous, nextStartup);
        }
        return () -> finishResume(nextStartup, captured);
    }

    private synchronized void finishResume(UUID expectedStartup, List<Connection> captured) {
        if (!expectedStartup.equals(startupId) || !writesAllowed.getAsBoolean()) return;
        // Include joins after the handoff snapshot and quits during handoff.
        // Each captured connection remains observable even after removal from
        // the active map, so a late quit is persisted as closed, not resurrected.
        Set<Connection> flush = new HashSet<>(captured);
        flush.addAll(connections.values());
        for (Connection connection : flush) {
            synchronized (connection) {
                emit(connection);
            }
        }
        knownStartups.clear();
        knownStartups.add(expectedStartup);
    }

    /** Caller holds the connection lock; submit only fills a ring slot, never performs DB I/O. */
    private void emit(Connection connection) {
        if (!writesAllowed.getAsBoolean()) return;
        UUID owner = startupId;
        ClientMeta meta = connection.meta;
        long activity = connection.lastActivity;
        long closedAt = connection.closedAt;
        store.submit(Categories.SESSION, event -> event
                .sessionId(connection.sessionId).playerUuid(connection.player)
                .startedEpochMs(connection.started).lastActivityEpochMs(activity).closedAtEpochMs(closedAt)
                .clientBrand(meta.clientBrand()).clientVersion(meta.clientVersion()).startupId(owner));
        connection.lastEmitted = activity;
    }

    private static ClientMeta mergeMeta(ClientMeta current, ClientMeta incoming) {
        return new ClientMeta(
                incoming.grimVersion() != null ? incoming.grimVersion() : current.grimVersion(),
                current.clientBrand() != null ? current.clientBrand() : incoming.clientBrand(),
                incoming.clientVersion() >= 0 ? incoming.clientVersion() : current.clientVersion(),
                incoming.serverVersion() != null ? incoming.serverVersion() : current.serverVersion());
    }

    private static final class Connection {
        final UUID player;
        final UUID sessionId = UUID.randomUUID();
        final long started;
        long lastActivity;
        long lastEmitted;
        long closedAt = SessionRecord.OPEN;
        ClientMeta meta;

        Connection(UUID player, long now, ClientMeta meta) {
            this.player = player;
            this.started = now;
            this.lastActivity = now;
            this.meta = meta;
        }
    }
}
