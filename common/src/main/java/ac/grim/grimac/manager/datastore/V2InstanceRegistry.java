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
    private final Logger logger;

    V2InstanceRegistry(
            @NotNull DataStore store,
            @NotNull StorageEventHandler<ServerStartupEvent> directStartupWriter,
            @NotNull Logger logger) {
        this.store = store;
        this.directStartupWriter = directStartupWriter;
        this.logger = logger;
    }

    static @Nullable V2InstanceRegistry create(
            @NotNull DataStore store,
            @NotNull V2Routes routes,
            @NotNull Logger logger) {
        V2Routes.Route<?> startups = routes.routeFor(STARTUPS);
        if (startups == null || !routes.contains(Categories.SESSION)) return null;
        return new V2InstanceRegistry(store, directWriter(startups, STARTUPS), logger);
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
            byte @Nullable [] verboseManifest) {
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
        return StartupClaim.enabled(startupId, instanceId, message);
    }

    long closeCurrentStartup(@NotNull UUID startupId, long closedAtEpochMs) {
        Optional<ServerStartupRecord> row = startupById(startupId);
        long closed = closeOpenSessions(startupId, closedAtEpochMs);
        row.ifPresent(startup -> markStartupClosed(startup, closedAtEpochMs, "graceful"));
        return closed;
    }

    /** Repairs every open startup the liveness rejects. Repeats change no rows, so peers need no coordination. */
    long recoverStaleStartups(@NotNull UUID currentStartupId, @NotNull LeaseStartupLiveness liveness) {
        liveness.refresh();
        long closed = 0L;
        Cursor cursor = null;
        do {
            Page<ServerStartupRecord> page = await(store.execute(new EntityOps.FindByIndexOp<>(
                    STARTUPS, "by_open_heartbeat", ServerStartupRecord.OPEN, cursor, PAGE_SIZE)),
                    "query open server startups");
            for (ServerStartupRecord row : page.items()) {
                if (currentStartupId.equals(row.startupId()) || liveness.isAlive(row)) continue;
                closed += recoverStartup(row, liveness.lastSeenEpochMs(row));
            }
            cursor = page.nextCursor();
        } while (cursor != null);
        return closed;
    }

    private long recoverStartup(@NotNull ServerStartupRecord startup, long lastSeenEpochMs) {
        long closed = closeOpenSessions(startup.startupId(), null);
        markStartupClosed(startup, lastSeenEpochMs, "stale");
        if (closed > 0) {
            logger.warning("[grim-datastore] recovered startupId=" + startup.startupId()
                    + " serverName='" + startup.serverName() + "' and closed " + closed + " open session(s).");
        }
        return closed;
    }

    /** One conditional update over the startup's open sessions. A null close time copies each row's last_activity. */
    private long closeOpenSessions(@NotNull UUID startupId, @Nullable Long closedAtEpochMs) {
        return await(store.execute(new EntityOps.SetIfSentinelOp(Categories.SESSION, "by_startup_open", startupId,
                "closed_at", SessionRecord.OPEN, closedAtEpochMs, closedAtEpochMs == null ? "last_activity" : null)),
                "close open sessions for startup " + startupId);
    }

    private void markStartupClosed(@NotNull ServerStartupRecord source, long closedAtEpochMs, @NotNull String reason) {
        ServerStartupRecord closed = new ServerStartupRecord(
                source.startupId(),
                source.instanceId(),
                source.serverName(),
                source.grimVersion(),
                source.serverVersionString(),
                source.hostname(),
                source.startedEpochMs(),
                Math.max(source.lastHeartbeatEpochMs(), closedAtEpochMs),
                closedAtEpochMs,
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
        return await(store.execute(new EntityOps.GetByIdOp<>(
                STARTUPS, startupId)), "query server startup " + startupId);
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
            @NotNull String warningMessage) {

        static @NotNull StartupClaim enabled(
                @NotNull UUID startupId,
                @NotNull UUID instanceId,
                @NotNull String message) {
            return new StartupClaim(true, false, startupId, instanceId, null, -1L, message);
        }

        static @NotNull StartupClaim duplicate(
                @NotNull UUID startupId,
                @NotNull UUID instanceId,
                @NotNull UUID conflictingStartupId,
                long heartbeatAgeMs,
                @NotNull String message) {
            return new StartupClaim(false, true, startupId, instanceId, conflictingStartupId,
                    heartbeatAgeMs, message);
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
