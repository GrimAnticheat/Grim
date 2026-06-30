package ac.grim.grimac.manager.datastore;

import ac.grim.grimac.api.storage.DataStore;
import ac.grim.grimac.api.storage.backend.ApiVersion;
import ac.grim.grimac.api.storage.backend.Backend;
import ac.grim.grimac.api.storage.backend.BackendContext;
import ac.grim.grimac.api.storage.backend.BackendException;
import ac.grim.grimac.api.storage.backend.KindAdapter;
import ac.grim.grimac.api.storage.backend.StorageEventHandler;
import ac.grim.grimac.api.storage.category.Capability;
import ac.grim.grimac.api.storage.category.Categories;
import ac.grim.grimac.api.storage.category.Category;
import ac.grim.grimac.api.storage.check.CheckCatalogPersistence;
import ac.grim.grimac.api.storage.check.CheckCatalogRepairResult;
import ac.grim.grimac.api.storage.event.ServerStartupEvent;
import ac.grim.grimac.api.storage.event.SessionEvent;
import ac.grim.grimac.api.storage.kind.ops.EntityOps;
import ac.grim.grimac.api.storage.model.ServerStartupRecord;
import ac.grim.grimac.api.storage.model.SessionRecord;
import ac.grim.grimac.api.storage.query.Cursor;
import ac.grim.grimac.api.storage.query.DeleteCriteria;
import ac.grim.grimac.api.storage.query.Page;
import ac.grim.grimac.api.storage.query.Query;
import ac.grim.grimac.internal.storage.checks.InMemoryCheckCatalogPersistence;
import ac.grim.grimac.internal.storage.core.V2Routes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

final class V2InstanceRegistry {

    static final Category<ServerStartupEvent> STARTUPS = Categories.SERVER_STARTUP;
    static final Backend ROUTER_SENTINEL_BACKEND = new RouterSentinelBackend();

    private static final int PAGE_SIZE = 512;

    private final DataStore store;
    private final StorageEventHandler<ServerStartupEvent> directStartupWriter;
    private final StorageEventHandler<SessionEvent> directSessionWriter;
    private final Logger logger;

    private V2InstanceRegistry(
            @NotNull DataStore store,
            @NotNull V2Routes.Route<?> startupsRoute,
            @NotNull V2Routes.Route<?> sessionsRoute,
            @NotNull Logger logger) {
        this.store = store;
        this.directStartupWriter = directWriter(startupsRoute, STARTUPS);
        this.directSessionWriter = directWriter(sessionsRoute, Categories.SESSION);
        this.logger = logger;
    }

    static @Nullable V2InstanceRegistry create(
            @NotNull DataStore store,
            @NotNull V2Routes routes,
            @NotNull Logger logger) {
        V2Routes.Route<?> startups = routes.routeFor(STARTUPS);
        V2Routes.Route<?> sessions = routes.routeFor(Categories.SESSION);
        if (startups == null || sessions == null) return null;
        return new V2InstanceRegistry(store, startups, sessions, logger);
    }

    void publish(@NotNull ServerStartupEvent source) {
        writeStartup(source);
    }

    StartupClaim openStartup(
            @NotNull String serverName,
            @NotNull UUID instanceId,
            @NotNull UUID startupId,
            @NotNull UUID fence,
            long startedEpochMs,
            long dbNowEpochMs,
            @Nullable String hostname,
            @Nullable String grimVersion,
            @Nullable String serverVersionString,
            @Nullable byte[] verboseManifest) {
        ServerStartupRecord current = new ServerStartupRecord(
                startupId,
                instanceId,
                serverName,
                grimVersion,
                serverVersionString,
                hostname,
                startedEpochMs,
                dbNowEpochMs,
                ServerStartupRecord.OPEN,
                null,
                verboseManifest);
        writeStartup(current);
        String message = "[grim-datastore] storage startup claimed: serverName='" + serverName
                + "' instanceId=" + instanceId
                + " startupId=" + startupId
                + " fence=" + fence + ".";
        logger.info(message);
        return StartupClaim.enabled(startupId, instanceId, 0L, message);
    }

    long closeCurrentStartup(@NotNull UUID startupId, long closedAtEpochMs) {
        Optional<ServerStartupRecord> row = startupById(startupId);
        long closed = closeOpenSessions(startupId, closedAtEpochMs);
        row.ifPresent(startup -> markStartupClosed(startup, closedAtEpochMs, "graceful"));
        return closed;
    }

    long recoverStaleStartups(
            @NotNull UUID currentStartupId,
            long now,
            long staleThresholdMs) {
        long closed = 0L;
        Cursor cursor = null;
        boolean stop = false;
        do {
            Page<ServerStartupRecord> page = await(store.execute(new EntityOps.FindByIndexOp<>(
                    STARTUPS,
                    "by_open_heartbeat",
                    ServerStartupRecord.OPEN,
                    cursor,
                    PAGE_SIZE)), "query stale server startups");
            for (ServerStartupRecord row : page.items()) {
                if (row.isClosed()) continue;
                if (!isStale(row, now, staleThresholdMs)) {
                    stop = true;
                    break;
                }
                if (currentStartupId.equals(row.startupId())) continue;
                closed += recoverStartup(row, "stale");
            }
            cursor = stop ? null : page.nextCursor();
        } while (cursor != null);
        return closed;
    }

    long recoverStartup(@NotNull UUID startupId, @NotNull String reason) {
        Optional<ServerStartupRecord> startup = startupById(startupId);
        return startup.map(row -> recoverStartup(row, reason)).orElse(0L);
    }

    private long recoverStartup(@NotNull ServerStartupRecord startup, @NotNull String reason) {
        long closed = closeOpenSessions(startup.startupId(), SessionRecord.OPEN);
        long closeAt = Math.max(startup.startedEpochMs(), startup.lastHeartbeatEpochMs());
        markStartupClosed(startup, closeAt, reason);
        if (closed > 0) {
            logger.warning("[grim-datastore] recovered startupId=" + startup.startupId()
                    + " serverName='" + startup.serverName() + "' reason=" + reason
                    + " and closed " + closed + " open session(s).");
        }
        return closed;
    }

    private long closeOpenSessions(@NotNull UUID startupId, long closedAtEpochMs) {
        long closed = 0L;
        Cursor cursor = null;
        boolean reachedClosedRows;
        do {
            reachedClosedRows = false;
            Page<SessionRecord> page = await(store.execute(new EntityOps.FindByIndexOp<>(
                    Categories.SESSION,
                    "by_startup_open",
                    startupId,
                    cursor,
                    PAGE_SIZE)), "query open sessions for startup " + startupId);
            for (SessionRecord session : page.items()) {
                if (!startupId.equals(session.startupId())) continue;
                if (session.isClosed()) {
                    reachedClosedRows = true;
                    continue;
                }
                long closeAt = closedAtEpochMs == SessionRecord.OPEN
                        ? session.lastActivityEpochMs()
                        : closedAtEpochMs;
                closeSession(session, closeAt);
                closed++;
            }
            cursor = reachedClosedRows ? null : page.nextCursor();
        } while (cursor != null);
        return closed;
    }

    private void closeSession(@NotNull SessionRecord source, long closedAtEpochMs) {
        SessionEvent event = new SessionEvent()
                .sessionId(source.sessionId())
                .playerUuid(source.playerUuid())
                .startedEpochMs(source.startedEpochMs())
                .lastActivityEpochMs(source.lastActivityEpochMs())
                .closedAtEpochMs(closedAtEpochMs)
                .clientBrand(source.clientBrand())
                .clientVersion(source.clientVersion())
                .startupId(source.startupId());
        try {
            directSessionWriter.onEvent(event, 0L, true);
        } catch (Exception e) {
            throw new RuntimeException("failed to close session " + source.sessionId(), e);
        }
    }

    private void markStartupClosed(
            @NotNull ServerStartupRecord source,
            long closedAtEpochMs,
            @NotNull String reason) {
        long closeAt = closedAtEpochMs == ServerStartupRecord.OPEN
                ? Math.max(source.startedEpochMs(), source.lastHeartbeatEpochMs())
                : closedAtEpochMs;
        ServerStartupRecord closed = new ServerStartupRecord(
                source.startupId(),
                source.instanceId(),
                source.serverName(),
                source.grimVersion(),
                source.serverVersionString(),
                source.hostname(),
                source.startedEpochMs(),
                Math.max(source.lastHeartbeatEpochMs(), closeAt),
                closeAt,
                reason,
                source.verboseManifest());
        writeStartup(closed);
    }

    private void writeStartup(@NotNull ServerStartupRecord source) {
        ServerStartupEvent event = new ServerStartupEvent()
                .startupId(source.startupId())
                .instanceId(source.instanceId())
                .serverName(source.serverName())
                .startedEpochMs(source.startedEpochMs())
                .lastHeartbeatEpochMs(source.lastHeartbeatEpochMs())
                .hostname(source.hostname())
                .grimVersion(source.grimVersion())
                .serverVersionString(source.serverVersionString())
                .verboseManifest(source.verboseManifest())
                .closedAtEpochMs(source.closedAtEpochMs())
                .closeReason(source.closeReason());
        writeStartup(event);
    }

    private void writeStartup(@NotNull ServerStartupEvent source) {
        try {
            directStartupWriter.onEvent(source, 0L, true);
        } catch (Exception e) {
            throw new RuntimeException("server startup write failed for " + source.startupId(), e);
        }
    }

    private @NotNull Optional<ServerStartupRecord> startupById(@NotNull UUID startupId) {
        return await(store.execute(new EntityOps.GetByIdOp<UUID, ServerStartupRecord>(
                STARTUPS, startupId)), "query server startup " + startupId);
    }

    private static boolean isStale(@NotNull ServerStartupRecord row, long now, long staleThresholdMs) {
        return heartbeatAgeMs(now, row) > staleThresholdMs;
    }

    private static long heartbeatAgeMs(long now, @NotNull ServerStartupRecord row) {
        return Math.max(0L, now - row.lastHeartbeatEpochMs());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <E> @NotNull StorageEventHandler<E> directWriter(
            @NotNull V2Routes.Route<?> route,
            @NotNull Category<E> category) {
        KindAdapter adapter = route.adapter();
        return adapter.writeHandler(route.storeId(), route.kind(), category);
    }

    private static <T> T await(@NotNull java.util.concurrent.CompletionStage<T> stage,
                               @NotNull String action) {
        try {
            return stage.toCompletableFuture().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(action + " interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof CompletionException ce && ce.getCause() != null) cause = ce.getCause();
            throw new RuntimeException(action + " failed", cause);
        }
    }

    record StartupClaim(
            boolean storageEnabled,
            boolean duplicate,
            @NotNull UUID startupId,
            @NotNull UUID instanceId,
            @Nullable UUID conflictingStartupId,
            long heartbeatAgeMs,
            long sessionsClosed,
            @NotNull String warningMessage) {

        static @NotNull StartupClaim enabled(
                @NotNull UUID startupId,
                @NotNull UUID instanceId,
                long sessionsClosed,
                @NotNull String message) {
            return new StartupClaim(true, false, startupId, instanceId, null,
                    -1L, sessionsClosed, message);
        }

        static @NotNull StartupClaim duplicate(
                @NotNull UUID startupId,
                @NotNull UUID instanceId,
                @NotNull UUID conflictingStartupId,
                long heartbeatAgeMs,
                @NotNull String message) {
            return new StartupClaim(false, true, startupId, instanceId, conflictingStartupId,
                    heartbeatAgeMs, 0L, message);
        }
    }

    @SuppressWarnings("deprecation")
    private static final class RouterSentinelBackend implements Backend {
        private final CheckCatalogPersistence checkCatalog = new InMemoryCheckCatalogPersistence();

        @Override public @NotNull String id() { return "__v2_startup_registry"; }
        @Override public @NotNull ApiVersion getApiVersion() { return ApiVersion.CURRENT; }
        @Override public @NotNull EnumSet<Capability> capabilities() { return EnumSet.noneOf(Capability.class); }
        @Override public @NotNull Set<Category<?>> supportedCategories() { return new HashSet<>(); }
        @Override public void init(@NotNull BackendContext ctx) {}
        @Override public @NotNull CheckCatalogPersistence checkCatalog() { return checkCatalog; }

        @Override
        public @NotNull CheckCatalogRepairResult repairCheckCatalog(
                @NotNull Map<Integer, Integer> legacyToCatalogCheckIds,
                @Nullable String introducedVersionReplacement) {
            return new CheckCatalogRepairResult(0, 0L, 0L);
        }

        @Override public void flush() {}
        @Override public void close() {}

        @Override
        public @NotNull <E> StorageEventHandler<E> eventHandlerFor(@NotNull Category<E> cat) throws BackendException {
            throw new BackendException("sentinel backend has no legacy event handlers");
        }

        @Override
        public @NotNull <R> Page<R> read(@NotNull Category<?> cat, @NotNull Query<R> query) throws BackendException {
            throw new BackendException("sentinel backend has no legacy read path");
        }

        @Override
        public <E> void delete(@NotNull Category<E> cat, @NotNull DeleteCriteria criteria) throws BackendException {
            throw new BackendException("sentinel backend has no legacy delete path");
        }

        @Override public long countViolationsInSession(@NotNull UUID sessionId) { return 0L; }
    }
}
