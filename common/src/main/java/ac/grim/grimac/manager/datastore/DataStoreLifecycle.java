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
import ac.grim.grimac.api.storage.backend.BackendRegistry;
import ac.grim.grimac.api.storage.backend.BackendV2;
import ac.grim.grimac.api.storage.category.Categories;
import ac.grim.grimac.api.storage.category.Category;
import ac.grim.grimac.api.storage.check.CheckCatalogPersistence;
import ac.grim.grimac.api.storage.config.DataStoreConfig;
import ac.grim.grimac.api.storage.history.HistoryService;
import ac.grim.grimac.api.storage.identity.NameResolver;
import ac.grim.grimac.api.storage.identity.NameResolverLink;
import ac.grim.grimac.api.storage.registry.MigrationContext;
import ac.grim.grimac.api.storage.registry.StoreId;
import ac.grim.grimac.api.storage.submit.ViolationSink;
import ac.grim.grimac.internal.storage.backend.mongo.MongoBackendConfig;
import ac.grim.grimac.internal.storage.backend.mongo.v2.MongoBackendV2;
import ac.grim.grimac.internal.storage.backend.mongo.v2.MongoMigrationContext;
import ac.grim.grimac.internal.storage.backend.mysql.MysqlBackendConfig;
import ac.grim.grimac.internal.storage.backend.mysql.v2.MysqlBackendV2;
import ac.grim.grimac.internal.storage.backend.postgres.PostgresBackendConfig;
import ac.grim.grimac.internal.storage.backend.postgres.v2.PostgresBackendV2;
import ac.grim.grimac.internal.storage.backend.redis.RedisBackendConfig;
import ac.grim.grimac.internal.storage.backend.redis.v2.RedisBackendV2;
import ac.grim.grimac.internal.storage.backend.sqlite.SqliteBackend;
import ac.grim.grimac.internal.storage.backend.sqlite.SqliteBackendConfig;
import ac.grim.grimac.internal.storage.backend.sqlite.v2.SqliteBackendV2;
import ac.grim.grimac.internal.storage.category.V2BuiltinKinds;
import ac.grim.grimac.internal.storage.checks.CheckRegistry;
import ac.grim.grimac.internal.storage.checks.InMemoryCheckCatalogPersistence;
import ac.grim.grimac.internal.storage.checks.JdbcCheckCatalogPersistence;
import ac.grim.grimac.internal.storage.core.CategoryRouter;
import ac.grim.grimac.internal.storage.core.DataStoreImpl;
import ac.grim.grimac.internal.storage.core.V2BackendBootstrap;
import ac.grim.grimac.internal.storage.core.V2Routes;
import ac.grim.grimac.internal.storage.history.HistoryServiceImpl;
import ac.grim.grimac.internal.storage.identity.LocalCacheLink;
import ac.grim.grimac.internal.storage.identity.NameResolverChain;
import ac.grim.grimac.internal.storage.identity.OfflineModeUuidLink;
import ac.grim.grimac.internal.storage.identity.PlayerIdentityService;
import ac.grim.grimac.internal.storage.instance.HeartbeatScheduler;
import ac.grim.grimac.internal.storage.migrate.LegacyMigrator;
import ac.grim.grimac.internal.storage.migrate.V0Reader;
import ac.grim.grimac.internal.storage.retention.RetentionSweeper;
import ac.grim.grimac.internal.storage.submit.ViolationSinkImpl;
import ac.grim.grimac.internal.storage.verbose.VerboseManifest;
import com.mongodb.client.MongoDatabase;
import org.bson.BsonBinarySubType;
import org.bson.Document;
import org.bson.types.Binary;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
    private SessionTracker sessionTracker = SessionTracker.NOOP;
    private LiveWriteHooks liveWriteHooks = LiveWriteHooks.NOOP;
    private PlayerToggleStore playerToggleStore = PlayerToggleStore.NOOP;
    private V2InstanceRegistry instanceRegistry;
    private HeartbeatScheduler heartbeatScheduler;
    private UUID instanceId;
    private UUID startupId;
    private long startupStartedEpochMs;
    private ScheduledExecutorService duplicateWarningExecutor;

    private boolean enabled = true;
    private boolean loaded;

    private final List<BackendV2> v2Backends = new ArrayList<>();

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
            this.loaded = buildAndStart(dataFolder);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[grim-datastore] failed to initialise storage — falling back to disabled", e);
            this.enabled = false;
            try { teardown(); } catch (Exception ignore) {}
        }
    }

    private boolean buildAndStart(Path dataFolder) throws Exception {
        V2Routes.Builder routesBuilder = V2Routes.builder();
        int allFailures = 0;

        Map<String, BackendV2> v2ById = new LinkedHashMap<>();
        for (Map.Entry<String, BackendConfig> entry : config.backends().entrySet()) {
            String backendId = entry.getKey();
            BackendConfig backendConfig = entry.getValue();
            BackendV2 v2 = constructV2Direct(backendId, backendConfig);
            if (v2 == null) {
                logger.warning("[grim-datastore] no v2 backend for id '" + backendId
                        + "' — categories routed here will be unavailable");
                continue;
            }
            try {
                v2.init(new SimpleContext(backendConfig, logger, dataFolder));
            } catch (Exception e) {
                logger.log(Level.SEVERE,
                        "[grim-datastore] v2 backend init failed for '" + backendId + "'", e);
                try { v2.close(); }
                catch (Exception closeFailure) {
                    logger.log(Level.WARNING,
                            "[grim-datastore] v2 backend close after failed init failed for '"
                                    + backendId + "'", closeFailure);
                }
                continue;
            }
            this.v2Backends.add(v2);
            v2ById.put(backendId, v2);
        }

        ensureCheckCatalogStore(v2ById);
        this.checkRegistry = buildCheckRegistry(dataFolder);

        for (Map.Entry<Category<?>, String> r : config.routing().entrySet()) {
            Category<?> cat = r.getKey();
            String backendId = r.getValue();
            if ("none".equals(backendId)) continue;
            BackendV2 v2 = v2ById.get(backendId);
            if (v2 == null) continue;

            Map<Category<?>, V2BackendBootstrap.Binding<?>> bindings = bindingsForCategory(cat);
            if (bindings.isEmpty()) continue;

            MigrationContext mctx = buildMigrationContext(v2);
            if (mctx == null) mctx = NO_OP_MIGRATION_CONTEXT;

            V2BackendBootstrap.Result result = V2BackendBootstrap.install(
                    bindings, v2, mctx, routesBuilder, logger);
            allFailures += result.failures().size();
            if (!result.ok()) {
                logger.warning("[grim-datastore] v2 bootstrap for '" + backendId
                        + "' had " + result.failures().size() + " failure(s):\n  - "
                        + String.join("\n  - ", result.failures()));
            }
        }

        boolean startupRouteInstalled = false;
        String sessionBackendId = config.routing().get(Categories.SESSION);
        if (sessionBackendId != null && !"none".equals(sessionBackendId)) {
            BackendV2 sessionBackend = v2ById.get(sessionBackendId);
            if (sessionBackend != null) {
                MigrationContext mctx = buildMigrationContext(sessionBackend);
                if (mctx == null) mctx = NO_OP_MIGRATION_CONTEXT;

                Map<Category<?>, V2BackendBootstrap.Binding<?>> bindings = Map.of(
                        V2InstanceRegistry.STARTUPS,
                        new V2BackendBootstrap.Binding<>(
                                StoreId.grim("server_startups"), V2BuiltinKinds.serverStartups()));
                V2BackendBootstrap.Result result = V2BackendBootstrap.install(
                        bindings, sessionBackend, mctx, routesBuilder, logger);
                allFailures += result.failures().size();
                if (result.ok()) {
                    startupRouteInstalled = true;
                } else {
                    logger.warning("[grim-datastore] v2 bootstrap for server startup registry on '"
                            + sessionBackendId + "' had " + result.failures().size() + " failure(s):\n  - "
                            + String.join("\n  - ", result.failures()));
                }
            }
        }

        if (allFailures > 0) {
            logger.severe("[grim-datastore] v2 cutover had " + allFailures
                    + " failure(s) — aborting storage init");
            closeV2Backends();
            throw new RuntimeException("v2 cutover failed with " + allFailures + " error(s)");
        }

        V2Routes routes = routesBuilder.build();
        if (routes.isEmpty()) {
            throw new RuntimeException("no v2 routes installed");
        }
        CategoryRouter router = startupRouteInstalled
                ? new CategoryRouter(Map.of(V2InstanceRegistry.STARTUPS, V2InstanceRegistry.ROUTER_SENTINEL_BACKEND))
                : new CategoryRouter(Map.of());
        this.dataStore = new DataStoreImpl(router, config.writePath(), logger);
        this.dataStore.withV2Routes(routes);
        this.dataStore.start();

        logger.info("[grim-datastore] v2 cutover complete: " + v2ById.size()
                + " v2 backend(s), " + routes + " routes installed, 0 legacy backends");

        if (!buildServices(routes)) {
            disableStorageAfterDuplicate();
            return false;
        }
        return true;
    }

    private boolean buildServices(@NotNull V2Routes routes) {
        V2InstanceRegistry.StartupClaim claim = startInstanceRegistry();
        if (claim != null && claim.duplicate()) {
            startDuplicateWarning(claim.warningMessage());
            return false;
        }

        boolean sessionRouted = routes.contains(Categories.SESSION);
        boolean violationRouted = routes.contains(Categories.VIOLATION);
        boolean playerIdentityRouted = routes.contains(Categories.PLAYER_IDENTITY);
        boolean settingRouted = routes.contains(Categories.SETTING);

        if (sessionRouted && violationRouted) {
            this.historyService = new HistoryServiceImpl(dataStore, checkRegistry,
                    config.history().entriesPerPage(), config.history().groupIntervalMs())
                    .withV2Startups(Categories.SERVER_STARTUP);
        } else {
            logger.warning("[grim-datastore] history disabled; missing "
                    + missingRoutes(sessionRouted, "session", violationRouted, "violation"));
        }
        this.playerIdentityService = new PlayerIdentityService(dataStore);
        this.nameResolver = buildNameResolver(dataStore, config.nameResolutionChain(), playerIdentityRouted);
        this.violationSink = violationRouted ? new ViolationSinkImpl(dataStore) : null;
        this.retentionSweeper = new RetentionSweeper(dataStore, config.retention(), logger);
        if (sessionRouted) {
            this.sessionTracker = new SessionTrackerImpl(
                    dataStore, config.serverName(), config.session().heartbeatIntervalMs(), startupId);
        } else {
            this.sessionTracker = SessionTracker.NOOP;
            logger.warning("[grim-datastore] session tracking disabled; missing session route");
        }
        if (sessionRouted && violationRouted) {
            this.liveWriteHooks = new LiveWriteHooksImpl(
                    dataStore, playerIdentityService, checkRegistry, sessionTracker);
        } else if (playerIdentityRouted) {
            this.liveWriteHooks = new IdentityLiveWriteHooks(playerIdentityService);
        } else {
            this.liveWriteHooks = LiveWriteHooks.NOOP;
        }
        if (settingRouted) {
            this.playerToggleStore = new PlayerToggleStoreImpl(dataStore, logger);
        } else {
            this.playerToggleStore = PlayerToggleStore.NOOP;
            logger.warning("[grim-datastore] player toggle persistence disabled; missing setting route");
        }
        return true;
    }

    private static @NotNull String missingRoutes(
            boolean firstPresent, @NotNull String first,
            boolean secondPresent, @NotNull String second) {
        if (!firstPresent && !secondPresent) return first + " and " + second + " routes";
        if (!firstPresent) return first + " route";
        return second + " route";
    }

    private @Nullable V2InstanceRegistry.StartupClaim startInstanceRegistry() {
        long heartbeatMs = instanceHeartbeatIntervalMs();
        this.instanceId = loadPersistentInstanceId(plugin.getDataFolder().toPath());
        this.startupId = UUID.randomUUID();
        this.startupStartedEpochMs = System.currentTimeMillis();

        this.instanceRegistry = V2InstanceRegistry.create(dataStore, dataStore.v2Routes(), logger);
        if (instanceRegistry == null) {
            logger.warning("[grim-datastore] v2 server startup registry route missing; "
                    + "startup ownership and instance heartbeats are disabled");
            return null;
        }

        byte[] verboseManifest = VerboseManifest.textOnly(VerboseManifest.FLAVOR_V2_PUBLIC);
        V2InstanceRegistry.StartupClaim claim = instanceRegistry.claimStartup(
                config.serverName(),
                instanceId,
                startupId,
                startupStartedEpochMs,
                hostname(),
                GrimAPI.INSTANCE.getExternalAPI().getGrimVersion(),
                serverVersionString(),
                verboseManifest,
                heartbeatMs);
        if (!claim.storageEnabled()) return claim;

        this.heartbeatScheduler = new HeartbeatScheduler(
                startupId,
                instanceId,
                config.serverName(),
                startupStartedEpochMs,
                hostname(),
                GrimAPI.INSTANCE.getExternalAPI().getGrimVersion(),
                serverVersionString(),
                verboseManifest,
                Duration.ofMillis(heartbeatMs),
                instanceRegistry::publish,
                logger);
        heartbeatScheduler.start();
        return claim;
    }

    private long instanceHeartbeatIntervalMs() {
        long configured = config.session().heartbeatIntervalMs();
        return configured > 0L ? configured : 30_000L;
    }

    private @NotNull UUID loadPersistentInstanceId(@NotNull Path dataFolder) {
        Path file = dataFolder.resolve("data").resolve("storage-instance.uuid");
        try {
            Files.createDirectories(file.getParent());
            if (Files.exists(file)) {
                String raw = Files.readString(file, StandardCharsets.UTF_8).trim();
                try {
                    return UUID.fromString(raw);
                } catch (IllegalArgumentException e) {
                    Path backup = file.resolveSibling(file.getFileName() + ".invalid-" + System.currentTimeMillis());
                    Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
                    logger.warning("[grim-datastore] invalid storage instance UUID in " + file
                            + "; moved it to " + backup + " and generated a new persistent id");
                }
            }

            UUID generated = UUID.randomUUID();
            Files.writeString(
                    file,
                    generated + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW);
            return generated;
        } catch (IOException e) {
            throw new IllegalStateException("failed to load persistent storage instance id from " + file, e);
        }
    }

    private @Nullable String serverVersionString() {
        return GrimAPI.INSTANCE.getPlatformServer().getPlatformImplementationString();
    }

    private void disableStorageAfterDuplicate() {
        logger.warning("[grim-datastore] storage disabled for this boot because another live Grim startup "
                + "is using this storage instance id. Runtime checks remain active.");
        if (heartbeatScheduler != null) {
            heartbeatScheduler.stop();
            heartbeatScheduler = null;
        }
        if (dataStore != null) {
            dataStore.flushAndClose(config.writePath().shutdownDrainTimeoutMs());
        }
        closeV2Backends();
        dataStore = null;
        historyService = null;
        playerIdentityService = null;
        nameResolver = null;
        violationSink = null;
        retentionSweeper = null;
        sessionTracker = SessionTracker.NOOP;
        liveWriteHooks = LiveWriteHooks.NOOP;
        playerToggleStore = PlayerToggleStore.NOOP;
        instanceRegistry = null;
        loaded = false;
        enabled = false;
    }

    private void startDuplicateWarning(@NotNull String message) {
        stopDuplicateWarning();
        logger.warning(message);
        duplicateWarningExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "grim-storage-duplicate-warning");
            t.setDaemon(true);
            return t;
        });
        duplicateWarningExecutor.scheduleAtFixedRate(
                () -> logger.warning(message),
                60L,
                60L,
                TimeUnit.SECONDS);
    }

    private void stopDuplicateWarning() {
        if (duplicateWarningExecutor != null) {
            duplicateWarningExecutor.shutdownNow();
            duplicateWarningExecutor = null;
        }
    }

    private @Nullable String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private @NotNull CheckRegistry buildCheckRegistry(@NotNull Path dataFolder) {
        String backendId = config.routing().get(Categories.VIOLATION);
        BackendConfig backendConfig = backendId == null ? null : config.backends().get(backendId);
        CheckCatalogPersistence persistence = checkCatalogPersistenceFor(dataFolder, backendConfig);
        if (persistence == null) {
            logger.warning("[grim-datastore] no persisted check catalog available for v2 backend '"
                    + backendId + "' — check names will be process-local only");
            persistence = new InMemoryCheckCatalogPersistence();
        }
        CheckRegistry registry = new CheckRegistry(persistence);
        try {
            registry.reload();
            return registry;
        } catch (RuntimeException e) {
            logger.log(Level.WARNING,
                    "[grim-datastore] failed to load persisted check catalog for backend '"
                            + backendId + "' — falling back to process-local check names", e);
            CheckRegistry fallback = new CheckRegistry(new InMemoryCheckCatalogPersistence());
            fallback.reload();
            return fallback;
        }
    }

    private @Nullable CheckCatalogPersistence checkCatalogPersistenceFor(
            @NotNull Path dataFolder, @Nullable BackendConfig backendConfig) {
        if (backendConfig instanceof SqliteBackendConfig c) {
            Path dbFile = dataFolder.resolve(c.path());
            return new JdbcCheckCatalogPersistence(
                    () -> DriverManager.getConnection("jdbc:sqlite:" + dbFile.toAbsolutePath()),
                    c.tableNames().checks());
        }
        if (backendConfig instanceof MysqlBackendConfig c) {
            return new JdbcCheckCatalogPersistence(
                    () -> DriverManager.getConnection(c.jdbcUrl(), c.user(),
                            c.password() == null ? "" : c.password()),
                    c.tableNames().checks());
        }
        if (backendConfig instanceof PostgresBackendConfig c) {
            return new JdbcCheckCatalogPersistence(
                    () -> DriverManager.getConnection(c.jdbcUrl(), c.user(),
                            c.password() == null ? "" : c.password()),
                    c.tableNames().checks());
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void ensureCheckCatalogStore(@NotNull Map<String, BackendV2> v2ById) {
        String backendId = config.routing().get(Categories.VIOLATION);
        if (backendId == null) return;
        BackendV2 backend = v2ById.get(backendId);
        if (backend == null) return;
        var kind = V2BuiltinKinds.checks();
        var adapterOpt = backend.adapterFor(kind);
        if (adapterOpt.isEmpty()) return;
        try {
            var adapter = (ac.grim.grimac.api.storage.backend.KindAdapter) adapterOpt.get();
            StoreId storeId = StoreId.grim("grim_checks");
            adapter.ensureStore(storeId, kind);
            MigrationContext mctx = buildMigrationContext(backend);
            if (mctx == null) mctx = NO_OP_MIGRATION_CONTEXT;
            for (Object mo : adapter.migrations(kind)) {
                ac.grim.grimac.api.storage.registry.Migration m =
                        (ac.grim.grimac.api.storage.registry.Migration) mo;
                m.apply(mctx, storeId, kind);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING,
                    "[grim-datastore] failed to ensure v2 check catalog store on backend '"
                            + backendId + "'", e);
        }
    }

    private @Nullable BackendV2 constructV2Direct(@NotNull String backendId,
                                                  @NotNull BackendConfig config) {
        return switch (backendId) {
            case "mongo" -> config instanceof MongoBackendConfig c ? new MongoBackendV2(c) : null;
            case "postgres" -> config instanceof PostgresBackendConfig c ? new PostgresBackendV2(c) : null;
            case "mysql" -> config instanceof MysqlBackendConfig c ? new MysqlBackendV2(c) : null;
            case "sqlite" -> config instanceof SqliteBackendConfig c ? new SqliteBackendV2(c) : null;
            case "redis" -> config instanceof RedisBackendConfig c ? new RedisBackendV2(c) : null;
            default -> null;
        };
    }

    private @NotNull Map<Category<?>, V2BackendBootstrap.Binding<?>> bindingsForCategory(
            @NotNull Category<?> cat) {
        Map<Category<?>, V2BackendBootstrap.Binding<?>> out = new LinkedHashMap<>();
        if (cat == Categories.VIOLATION) {
            out.put(cat, new V2BackendBootstrap.Binding<>(
                    StoreId.grim("grim_violations"), V2BuiltinKinds.violations()));
        } else if (cat == Categories.SESSION) {
            out.put(cat, new V2BackendBootstrap.Binding<>(
                    StoreId.grim("grim_sessions"), V2BuiltinKinds.sessions()));
        } else if (cat == Categories.PLAYER_IDENTITY) {
            out.put(cat, new V2BackendBootstrap.Binding<>(
                    StoreId.grim("grim_players"), V2BuiltinKinds.players()));
        } else if (cat == Categories.SETTING) {
            out.put(cat, new V2BackendBootstrap.Binding<>(
                    StoreId.grim("grim_settings"), V2BuiltinKinds.settings()));
        }
        return out;
    }

    private @Nullable MigrationContext buildMigrationContext(@NotNull BackendV2 backend) {
        if (backend instanceof MongoBackendV2 mongo) {
            MongoDatabase db = mongo.unwrap(MongoDatabase.class).orElse(null);
            if (db == null) return null;
            maybeWarnUnexpectedIdShape(db);
            return new MongoMigrationContext(db, logger);
        }
        if (backend instanceof PostgresBackendV2
                || backend instanceof MysqlBackendV2
                || backend instanceof SqliteBackendV2
                || backend instanceof RedisBackendV2) {
            return NO_OP_MIGRATION_CONTEXT;
        }
        return null;
    }

    private static final MigrationContext NO_OP_MIGRATION_CONTEXT =
            new MigrationContext() {};

    private void maybeWarnUnexpectedIdShape(@NotNull MongoDatabase db) {
        for (String coll : new String[]{"grim_sessions", "grim_players"}) {
            try {
                Document first = db.getCollection(coll).find().limit(1).first();
                if (first == null) continue;
                Object id = first.get("_id");
                if (id instanceof java.util.UUID) continue;
                if (id instanceof Binary b
                        && (b.getType() == BsonBinarySubType.BINARY.getValue()
                        || b.getType() == BsonBinarySubType.UUID_STANDARD.getValue())
                        && b.getData().length == 16) continue;
                String idClass = id == null ? "null" : id.getClass().getSimpleName();
                logger.warning(() -> "[grim-datastore] " + coll + " first-doc _id is "
                        + idClass + ", expected UUID-shaped binary —"
                        + " entity migration will not handle this row correctly."
                        + " Halt and inspect before proceeding if this is unexpected.");
            } catch (RuntimeException e) {
                logger.fine(() -> "[grim-datastore] _id sanity probe failed for " + coll
                        + ": " + e.getMessage());
            }
        }
    }

    private void closeV2Backends() {
        for (BackendV2 v2 : v2Backends) {
            try { v2.flush(); }
            catch (Exception e) {
                logger.log(Level.WARNING, "[grim-datastore] v2 flush failed for " + v2.id(), e);
            }
            try { v2.close(); }
            catch (Exception e) {
                logger.log(Level.WARNING, "[grim-datastore] v2 close failed for " + v2.id(), e);
            }
        }
        v2Backends.clear();
    }

    private NameResolver buildNameResolver(
            DataStore store,
            List<String> chain,
            boolean playerIdentityRouted) {
        List<NameResolverLink> links = new ArrayList<>();
        for (String id : chain) {
            switch (id) {
                case "local-cache" -> {
                    if (playerIdentityRouted) {
                        links.add(new LocalCacheLink(store));
                    } else {
                        logger.warning("[grim-datastore] name resolver local-cache disabled; "
                                + "missing player-identity route");
                    }
                }
                case "offline-mode-uuid" -> links.add(new OfflineModeUuidLink());
                default -> logger.warning("[grim-datastore] unknown name-resolver link: " + id);
            }
        }
        return new NameResolverChain(links);
    }

    private void maybeMigrateLegacy(Path dataFolder, SqliteBackend sqliteBackend) {
        // The V0 reader/import path is SQLite-only. Only run it when the
        // violation route itself is SQLite; mixed routing should not import
        // legacy violations into an unrelated local side database.
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
        stopDuplicateWarning();
        // violationSink drains in-flight writes; dataStore drains per-category
        // rings and closes each backend. Both null-guarded because a failure
        // during buildAndStart can tear down mid-initialisation — start()'s
        // catch calls teardown() before any of these fields were assigned.
        if (heartbeatScheduler != null) {
            heartbeatScheduler.stop();
            heartbeatScheduler = null;
        }
        shutdownInstanceRegistry();
        playerToggleStore.shutdown();
        if (violationSink != null) violationSink.shutDown();
        if (dataStore != null) {
            long drainMs = config != null ? config.writePath().shutdownDrainTimeoutMs() : 5000L;
            dataStore.flushAndClose(drainMs);
        }
        closeV2Backends();
        dataStore = null;
        historyService = null;
        playerIdentityService = null;
        nameResolver = null;
        violationSink = null;
        retentionSweeper = null;
        sessionTracker = SessionTracker.NOOP;
        liveWriteHooks = LiveWriteHooks.NOOP;
        playerToggleStore = PlayerToggleStore.NOOP;
        instanceRegistry = null;
        instanceId = null;
        startupId = null;
        startupStartedEpochMs = 0L;
        checkRegistry = null;
        config = null;
        loaded = false;
    }

    private void shutdownInstanceRegistry() {
        if (instanceRegistry == null || startupId == null || config == null) return;
        long now = System.currentTimeMillis();
        try {
            long closed = instanceRegistry.closeCurrentStartup(startupId, now);
            if (closed > 0) {
                logger.info("[grim-datastore] closed " + closed
                        + " still-open session(s) for this server startup");
            }
        } catch (RuntimeException e) {
            logger.log(Level.WARNING,
                    "[grim-datastore] failed to close sessions for this server startup", e);
        }
    }

    public boolean isEnabled() { return enabled; }
    public boolean isLoaded() { return loaded; }

    public @Nullable DataStore dataStore() { return loaded ? dataStore : null; }
    public @Nullable HistoryService historyService() { return historyService; }
    public @Nullable NameResolver nameResolver() { return nameResolver; }
    public @Nullable ViolationSink violationSink() { return violationSink; }
    public @Nullable DataStoreConfig config() { return config; }

    /**
     * The live-writes facade used by {@code PunishmentManager} and
     * {@code PacketPlayerJoinQuit}. Returns {@link LiveWriteHooks#NOOP} when
     * the datastore is disabled or its init failed — callers don't null-check.
     */
    public @NotNull LiveWriteHooks liveWriteHooks() { return liveWriteHooks; }

    /**
     * The live session tracker. Returns {@link SessionTracker#NOOP} when the
     * datastore is disabled or its init failed.
     */
    public @NotNull SessionTracker sessionTracker() { return sessionTracker; }

    /**
     * Persistence layer for the per-player /grim alerts | verbose | brands
     * toggles. Returns {@link PlayerToggleStore#NOOP} when the datastore is
     * disabled or its init failed.
     */
    public @NotNull PlayerToggleStore playerToggleStore() { return playerToggleStore; }

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
            if (b == V2InstanceRegistry.ROUTER_SENTINEL_BACKEND) continue;
            out.put(b.id(), b);
        }
        return out;
    }

    private record SimpleContext(BackendConfig config, Logger logger, Path dataDirectory) implements BackendContext {}
}
