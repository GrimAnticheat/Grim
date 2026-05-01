package ac.grim.grimac.manager.datastore;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.manager.init.start.StartableInitable;
import ac.grim.grimac.manager.init.stop.StoppableInitable;
import ac.grim.grimac.api.storage.DataStore;
import ac.grim.grimac.api.storage.backend.Backend;
import ac.grim.grimac.api.storage.backend.BackendConfig;
import ac.grim.grimac.api.storage.backend.BackendContext;
import ac.grim.grimac.api.storage.backend.BackendException;
import ac.grim.grimac.api.storage.backend.BackendProvider;
import ac.grim.grimac.api.storage.backend.BackendRegistry;
import ac.grim.grimac.api.storage.category.Category;
import ac.grim.grimac.api.storage.config.DataStoreConfig;
import ac.grim.grimac.api.storage.history.HistoryService;
import ac.grim.grimac.api.storage.identity.NameResolver;
import ac.grim.grimac.api.storage.identity.NameResolverLink;
import ac.grim.grimac.api.storage.submit.ViolationSink;
import ac.grim.grimac.internal.storage.backend.sqlite.SqliteBackend;
import ac.grim.grimac.internal.storage.checks.CheckRegistry;
import ac.grim.grimac.internal.storage.checks.SqliteCheckPersistence;
import ac.grim.grimac.internal.storage.core.CapabilityValidator;
import ac.grim.grimac.internal.storage.core.CategoryRouter;
import ac.grim.grimac.internal.storage.core.DataStoreImpl;
import ac.grim.grimac.internal.storage.history.HistoryServiceImpl;
import ac.grim.grimac.internal.storage.identity.LocalCacheLink;
import ac.grim.grimac.internal.storage.identity.NameResolverChain;
import ac.grim.grimac.internal.storage.identity.OfflineModeUuidLink;
import ac.grim.grimac.internal.storage.identity.PlayerIdentityService;
import ac.grim.grimac.internal.storage.migrate.LegacyMigrator;
import ac.grim.grimac.internal.storage.migrate.V0Reader;
import ac.grim.grimac.internal.storage.retention.RetentionSweeper;
import ac.grim.grimac.internal.storage.submit.ViolationSinkImpl;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wires the shared DataStore + associated services to the plugin's
 * start/stop lifecycle. Owns the construction order: build backends → init →
 * capability-validate routing → migrate any legacy store → start writer
 * loops → register services. Accepting players happens in
 * {@link GrimAPI#start()} after this.
 */
public final class DataStoreLifecycle implements StartableInitable, StoppableInitable {

    private final GrimPlugin plugin;
    private final Logger logger;
    private final BackendRegistry backendRegistry;

    private DataStoreConfig config;
    private DataStoreImpl dataStore;
    private CheckRegistry checkRegistry;
    private HistoryServiceImpl historyService;
    private PlayerIdentityService playerIdentityService;
    private NameResolver nameResolver;
    private ViolationSinkImpl violationSink;
    private RetentionSweeper retentionSweeper;
    private SessionTracker sessionTracker;
    private LiveWriteHooks liveWriteHooks;
    private PlayerToggleStore playerToggleStore;

    private boolean enabled = true;
    private boolean loaded;

    public DataStoreLifecycle(@NotNull GrimPlugin plugin, @NotNull BackendRegistry backendRegistry) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.backendRegistry = Objects.requireNonNull(backendRegistry, "backendRegistry");
        this.logger = Logger.getLogger("grim-datastore");
    }

    @Override
    public void start() {
        Path dataFolder = plugin.getDataFolder().toPath();
        // database.yml + per-backend files load through the shared
        // ConfigManager (see ConfigManagerFileImpl). Their key paths are
        // namespaced under `database:` / `<id>:` wrappers so Configuralize's
        // flat-merge doesn't collide them with config.yml / discord.yml.
        // The cross-version updater also runs there before this method is
        // called, so the on-disk files are already migrated.
        DataStoreConfigBuilder builder = new DataStoreConfigBuilder(
                backendRegistry,
                dataFolder,
                GrimAPI.INSTANCE.getConfigManager().getConfig());

        if (!builder.enabled()) {
            logger.info("[grim-datastore] disabled in database.yml — skipping storage init");
            this.enabled = false;
            return;
        }
        try {
            this.config = builder.build();
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "[grim-datastore] database.yml rejected — storage disabled", e);
            this.enabled = false;
            return;
        }

        try {
            buildAndStart(dataFolder);
            this.loaded = true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[grim-datastore] failed to initialise storage — falling back to disabled", e);
            this.enabled = false;
            try { teardown(); } catch (Exception ignore) {}
        }
    }

    private void buildAndStart(Path dataFolder) throws Exception {
        Map<String, Backend> backendsById = new LinkedHashMap<>();
        for (Map.Entry<String, BackendConfig> entry : config.backends().entrySet()) {
            Backend b = buildBackend(entry.getKey(), entry.getValue());
            b.init(new SimpleContext(entry.getValue(), logger, dataFolder));
            backendsById.put(entry.getKey(), b);
        }

        Map<Category<?>, Backend> routing = new LinkedHashMap<>();
        for (Map.Entry<Category<?>, String> r : config.routing().entrySet()) {
            Backend b = backendsById.get(r.getValue());
            if (b == null) continue; // routing target was "none" or an unknown id — skip this category
            routing.put(r.getKey(), b);
        }

        CapabilityValidator.validate(routing);

        // The CheckRegistry + legacy-migration paths still need a SQLite-backed
        // persistence store and a SQLite-specific V0 reader. When SQLite is
        // absent from routing (pure-memory tests, or a site that routes to a
        // non-SQL backend only), we degrade gracefully: CheckRegistry falls
        // back to in-memory, legacy migration is skipped.
        SqliteBackend sqliteForExtras = findSqliteIn(backendsById);
        if (sqliteForExtras != null) {
            this.checkRegistry = new CheckRegistry(new SqliteCheckPersistence(sqliteForExtras.jdbcUrl()));
            this.checkRegistry.reload();
        }

        maybeMigrateLegacy(dataFolder, sqliteForExtras);

        CategoryRouter router = new CategoryRouter(routing);
        this.dataStore = new DataStoreImpl(router, config.writePath(), logger);
        this.dataStore.start();

        if (checkRegistry == null) {
            this.checkRegistry = new CheckRegistry(new CheckRegistry.CheckPersistence() {
                @Override public Iterable<CheckRegistry.CheckRow> loadAll() { return List.of(); }
                @Override public int insert(String stableKey, String display, String description,
                                            String introducedVersion, long introducedAt) {
                    return stableKey.hashCode();
                }
                @Override public void updateDisplayAndDescription(int checkId, String display, String description) {}
            });
        }

        this.historyService = new HistoryServiceImpl(dataStore, checkRegistry,
                config.history().entriesPerPage(), config.history().groupIntervalMs());
        this.playerIdentityService = new PlayerIdentityService(dataStore);
        this.nameResolver = buildNameResolver(dataStore, config.nameResolutionChain());
        this.violationSink = new ViolationSinkImpl(dataStore);
        this.retentionSweeper = new RetentionSweeper(dataStore, config.retention(), logger);
        this.sessionTracker = new SessionTrackerImpl(
                dataStore, config.serverName(), config.session().heartbeatIntervalMs());
        this.liveWriteHooks = new LiveWriteHooksImpl(
                dataStore, playerIdentityService, checkRegistry, sessionTracker);
        this.playerToggleStore = new PlayerToggleStoreImpl(dataStore, logger);

        // Crash sweep — mark every session whose closed_at is still null as
        // crashed by stamping closed_at = last_activity. SessionTracker has
        // no in-memory state at this point (we're pre-first-join), so any
        // open row is by definition orphaned. Per-backend implementations
        // do this in one UPDATE; default no-op for backends without a
        // session table.
        for (Backend b : router.allBackends()) {
            try {
                long affected = b.markCrashedSessions();
                if (affected > 0) {
                    logger.info("[grim-datastore] marked " + affected
                            + " open session(s) on backend '" + b.id()
                            + "' as crashed (server didn't shut down cleanly last run)");
                }
            } catch (Exception e) {
                logger.log(Level.WARNING,
                        "[grim-datastore] markCrashedSessions failed on backend '"
                                + b.id() + "' — sessions may show as ongoing forever", e);
            }
        }
    }

    private Backend buildBackend(String id, BackendConfig cfg) {
        BackendProvider provider = backendRegistry.lookup(id);
        if (provider == null) {
            throw new IllegalArgumentException("no backend provider registered for id '" + id
                    + "' (registered: " + backendRegistry.registeredIds() + ")");
        }
        return provider.create(cfg);
    }

    private static SqliteBackend findSqliteIn(Map<String, Backend> backends) {
        for (Backend b : backends.values()) {
            if (b instanceof SqliteBackend s) return s;
        }
        return null;
    }

    private NameResolver buildNameResolver(DataStore store, List<String> chain) {
        List<NameResolverLink> links = new ArrayList<>();
        for (String id : chain) {
            switch (id) {
                case "local-cache" -> links.add(new LocalCacheLink(store));
                case "offline-mode-uuid" -> links.add(new OfflineModeUuidLink());
                default -> logger.warning("[grim-datastore] unknown name-resolver link: " + id);
            }
        }
        return new NameResolverChain(links);
    }

    private void maybeMigrateLegacy(Path dataFolder, SqliteBackend sqliteBackend) {
        // SqliteBackend is the only backend that currently understands the
        // legacy V0 layout. When routing doesn't include SQLite, there's no
        // place to migrate into — skip.
        if (sqliteBackend == null) return;
        if (config.migration().skip()) {
            logger.info("[grim-datastore] migration.skip=true; leaving legacy v0 un-migrated");
            return;
        }
        V0Sources.V0Source source = V0Sources.detect(
                dataFolder,
                GrimAPI.INSTANCE.getConfigManager().getConfig());
        // No legacy store on disk — fresh install or migration already done.
        if (source == null) {
            logger.info("[grim-datastore] no legacy v0 store detected; nothing to migrate");
            return;
        }
        logger.info("[grim-datastore] legacy v0 source: " + source.summary());
        V0Reader reader = new V0Reader(source.jdbcUrl(), source.username(), source.password());
        LegacyMigrator migrator = new LegacyMigrator(
                reader, sqliteBackend, checkRegistry,
                ClientVersionResolver::legacyStringToPvn,
                config.session().gapMs(), logger);
        long startMs = System.currentTimeMillis();
        try {
            LegacyMigrator.Result result = migrator.run(count -> {
                if (count % 5000 == 0) logger.info("[grim-datastore] migrated " + count + " violations so far");
            });
            long elapsed = System.currentTimeMillis() - startMs;
            logger.info("[grim-datastore] legacy migration: " + result.sessionsWritten() + " sessions, "
                    + result.violationsWritten() + " violations, " + elapsed + "ms"
                    + (result.resumed() ? " (resumed)" : ""));
        } catch (BackendException e) {
            logger.log(Level.SEVERE, "[grim-datastore] legacy migration failed", e);
        }
    }

    // Source detection moved to V0Sources so the /grim history migrate command
    // can reuse the same routing logic. See that class for per-type builders.

    @Override
    public void stop() {
        teardown();
    }

    /**
     * Hot-reload from a freshly-refreshed ConfigManager. Drains in-flight
     * writes within the configured {@code shutdown-drain-timeout-ms},
     * drops anything still pending, then rebuilds backends + routing
     * from the new {@code database.yml} / {@code databases/&lt;id&gt;.yml}.
     *
     * <p>Operators can swap the backend (e.g. SQLite → MySQL after a
     * {@code /grim history migrate}) without bouncing the server. Brief
     * unavailability between the drain and the new backend's init —
     * writes during that window get dropped on the floor; the user
     * accepts that tradeoff.
     *
     * <p>Stale references held by callers (e.g. a check that cached
     * {@link LiveWriteHooks} in a local variable mid-event) keep working
     * against the old, closed dataStore — those writes drop too. New
     * lookups via {@link #liveWriteHooks()} resolve to the new instance.
     */
    public synchronized void reload() {
        logger.info("[grim-datastore] /grim reload: tearing down datastore...");
        teardown();
        start();
    }

    /**
     * Idempotent teardown — drains writers, closes backends, nulls every
     * service field. Used by both {@link #stop()} and {@link #reload()}.
     * Doesn't touch {@code enabled}; {@link #start()} re-evaluates that
     * from the freshly-loaded ConfigManager.
     */
    private void teardown() {
        // violationSink drains in-flight writes; dataStore drains per-category
        // rings and closes each backend. Both null-guarded because a failure
        // during buildAndStart can tear down mid-initialisation — start()'s
        // catch calls teardown() before any of these fields were assigned.
        if (playerToggleStore != null) playerToggleStore.shutdown();
        if (violationSink != null) violationSink.shutDown();
        if (dataStore != null) {
            long drainMs = config != null ? config.writePath().shutdownDrainTimeoutMs() : 5000L;
            dataStore.flushAndClose(drainMs);
        }
        dataStore = null;
        historyService = null;
        playerIdentityService = null;
        nameResolver = null;
        violationSink = null;
        retentionSweeper = null;
        sessionTracker = null;
        liveWriteHooks = null;
        playerToggleStore = null;
        checkRegistry = null;
        config = null;
        loaded = false;
    }

    public boolean isEnabled() { return enabled; }
    public boolean isLoaded() { return loaded; }

    public @Nullable DataStore dataStore() { return dataStore; }
    public @Nullable HistoryService historyService() { return historyService; }
    public @Nullable NameResolver nameResolver() { return nameResolver; }
    public @Nullable ViolationSink violationSink() { return violationSink; }
    public @Nullable DataStoreConfig config() { return config; }

    /**
     * The live-writes facade used by {@code PunishmentManager} and
     * {@code PacketPlayerJoinQuit}. Returns {@link LiveWriteHooks#NOOP} when
     * the datastore is disabled or its init failed — callers don't null-check.
     */
    public @NotNull LiveWriteHooks liveWriteHooks() { return loaded ? liveWriteHooks : LiveWriteHooks.NOOP; }

    /**
     * The live session tracker. Returns {@link SessionTracker#NOOP} when the
     * datastore is disabled or its init failed.
     */
    public @NotNull SessionTracker sessionTracker() { return loaded ? sessionTracker : SessionTracker.NOOP; }

    /**
     * Persistence layer for the per-player /grim alerts | verbose | brands
     * toggles. Returns {@link PlayerToggleStore#NOOP} when the datastore is
     * disabled or its init failed.
     */
    public @NotNull PlayerToggleStore playerToggleStore() { return loaded ? playerToggleStore : PlayerToggleStore.NOOP; }

    /**
     * Admin-command escape hatch used by {@code /grim history migrate} to target
     * SQLite directly. Scans the active router for a {@link SqliteBackend}
     * instance; returns null when routing doesn't include one (e.g. pure-memory
     * test setups, or a site that routes everything to a non-SQL backend). The
     * migration command degrades gracefully in that case.
     */
    @ApiStatus.Internal
    public @Nullable SqliteBackend sqliteBackendForCommands() {
        if (dataStore == null) return null;
        for (Backend b : dataStore.router().allBackends()) {
            if (b instanceof SqliteBackend s) return s;
        }
        return null;
    }

    /**
     * Admin-command escape hatch. Returns the shared {@code CheckRegistry}
     * instance so {@code /grim history migrate} can intern stable keys through
     * the same registry the migrator uses at startup.
     */
    @ApiStatus.Internal
    public @Nullable CheckRegistry checkRegistryForCommands() {
        return checkRegistry;
    }

    /**
     * Admin-command escape hatch. Returns all backends currently wired into the
     * router, keyed by backend id. {@code /grim history copy} uses this to
     * resolve {@code <src>} / {@code <dst>} arguments against the same backend
     * instances the write path uses.
     */
    @ApiStatus.Internal
    public @NotNull Map<String, Backend> allBackendsForCommands() {
        if (dataStore == null) return Map.of();
        Map<String, Backend> out = new LinkedHashMap<>();
        for (Backend b : dataStore.router().allBackends()) {
            out.put(b.id(), b);
        }
        return out;
    }

    private record SimpleContext(BackendConfig config, Logger logger, Path dataDirectory) implements BackendContext {}
}
