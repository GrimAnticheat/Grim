package ac.grim.grimac.manager.datastore;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.checks.impl.verbose.VerboseCodecs;
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
import ac.grim.grimac.api.storage.check.CheckCatalogRow;
import ac.grim.grimac.api.storage.config.DataStoreConfig;
import ac.grim.grimac.api.storage.config.DuplicatePersistentUuidAction;
import ac.grim.grimac.api.storage.history.HistoryService;
import ac.grim.grimac.api.storage.identity.NameResolver;
import ac.grim.grimac.api.storage.identity.NameResolverLink;
import ac.grim.grimac.api.storage.instance.OwnershipClaimResult;
import ac.grim.grimac.api.storage.instance.ServerOwnershipAdapter;
import ac.grim.grimac.api.storage.instance.ServerOwnershipGate;
import ac.grim.grimac.api.storage.instance.ServerOwnershipMetadata;
import ac.grim.grimac.api.storage.instance.ServerOwnershipSnapshot;
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
import ac.grim.grimac.internal.storage.checks.DataStoreCheckCatalogPersistence;
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
import ac.grim.grimac.internal.storage.verbose.VerboseRegistry;
import ac.grim.grimac.internal.storage.verbose.VerboseRegistryImpl;
import ac.grim.grimac.manager.init.start.StartableInitable;
import ac.grim.grimac.manager.init.stop.StoppableInitable;
import com.mongodb.client.MongoDatabase;
import org.bson.BsonBinarySubType;
import org.bson.Document;
import org.bson.types.Binary;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
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
 * Wires the shared DataStore and services to the plugin lifecycle. This path
 * uses the v2 backend primitives so persistent UUID ownership, startup rows,
 * and crash recovery all share the same backend under the session route.
 */
public final class DataStoreLifecycle implements StartableInitable, StoppableInitable {

    private static final String CHECKS_STORE = "grim_checks";
    private static final StoreId OWNERSHIP_STORE = StoreId.grim("server_ownership");

    private final GrimPlugin plugin;
    private final Logger logger;
    private final BackendRegistry backendRegistry;

    private DataStoreConfig config;
    private DataStoreImpl dataStore;
    private CheckRegistry checkRegistry;
    private VerboseRegistry verboseRegistry;
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
    private OwnershipHeartbeatScheduler ownershipHeartbeatScheduler;
    private ServerOwnershipAdapter ownershipAdapter;
    private ServerOwnershipGate ownershipGate = ServerOwnershipGate.disabled();
    private UUID instanceId;
    private UUID startupId;
    private UUID ownershipFence;
    private long startupStartedEpochMs;
    private ScheduledExecutorService duplicateWarningExecutor;
    private ScheduledExecutorService recoverySweepExecutor;

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
        DataStoreConfigBuilder builder = new DataStoreConfigBuilder(
                backendRegistry,
                dataFolder,
                GrimAPI.INSTANCE.getConfigManager().getConfig());

        if (!builder.enabled()) {
            logger.info("[grim-datastore] disabled in database.yml - skipping storage init");
            this.enabled = false;
            installLocalVerboseRegistry();
            return;
        }
        this.enabled = true;
        try {
            this.config = builder.build();
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "[grim-datastore] database.yml rejected - storage disabled", e);
            this.enabled = false;
            installLocalVerboseRegistry();
            return;
        }

        try {
            this.loaded = buildAndStart(dataFolder);
        } catch (FatalStorageStartupException e) {
            logger.log(Level.SEVERE, "[grim-datastore] fatal storage startup failure - shutting down server", e);
            try { close(); } catch (Exception ignore) {}
            this.enabled = false;
            shutdownServerAfterFatalStorageStartup();
        } catch (Exception | LinkageError e) {
            logger.log(Level.SEVERE, "[grim-datastore] failed to initialise storage - falling back to disabled", e);
            try { close(); } catch (Exception ignore) {}
            this.enabled = false;
            installLocalVerboseRegistry();
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
                        + "' - categories routed here will be unavailable");
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
                this.ownershipAdapter = sessionBackend.ownershipAdapter().orElse(null);
                if (ownershipAdapter != null) {
                    try {
                        ownershipAdapter.ensureStore(OWNERSHIP_STORE);
                    } catch (Exception e) {
                        allFailures++;
                        logger.log(Level.WARNING,
                                "[grim-datastore] failed to ensure server ownership store on '"
                                        + sessionBackendId + "'", e);
                    }
                }
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
            logger.warning("[grim-datastore] v2 cutover skipped " + allFailures
                    + " failed route(s); continuing with successfully installed routes");
        }

        V2Routes routes = routesBuilder.build();
        if (routes.isEmpty()) {
            throw new RuntimeException("no v2 routes installed");
        }

        CategoryRouter router = startupRouteInstalled
                ? new CategoryRouter(Map.of(V2InstanceRegistry.STARTUPS, V2InstanceRegistry.ROUTER_SENTINEL_BACKEND))
                : new CategoryRouter(Map.of());
        boolean enforceOwnership = config.ownership().enforcePersistentUuidOwnership()
                && config.ownership().duplicatePersistentUuidAction() != DuplicatePersistentUuidAction.ALLOW_UNSAFE;
        this.ownershipGate = new ServerOwnershipGate(enforceOwnership);
        this.dataStore = new DataStoreImpl(router, config.writePath(), logger);
        this.dataStore.withV2Routes(routes);
        this.dataStore.withOwnershipGate(ownershipGate);
        this.dataStore.start();
        this.checkRegistry = buildCheckRegistry(v2ById);
        this.verboseRegistry = buildVerboseRegistry();

        logger.info("[grim-datastore] v2 cutover complete: " + v2ById.size()
                + " v2 backend(s), " + routes + " routes installed, 0 legacy backends");

        if (!buildServices(routes)) {
            disableStorageAfterDuplicate();
            return false;
        }
        return true;
    }

    private @NotNull CheckRegistry buildCheckRegistry(@NotNull Map<String, BackendV2> v2ById) {
        String backendId = config.routing().get(Categories.VIOLATION);
        BackendConfig backendConfig = backendId == null ? null : config.backends().get(backendId);
        BackendV2 backend = backendId == null ? null : v2ById.get(backendId);
        if (backend == null || !dataStore.v2Routes().contains(Categories.CHECK_CATALOG)) {
            logger.warning("[grim-datastore] no routed check catalog available for v2 backend '"
                    + backendId + "' - check names will be process-local only");
            CheckRegistry fallback = new CheckRegistry(new InMemoryCheckCatalogPersistence());
            fallback.reload();
            return fallback;
        }

        List<CheckCatalogRow> initialRows;
        try {
            initialRows = loadExistingCheckCatalogRows(backend, backendConfig);
        } catch (RuntimeException e) {
            logger.log(Level.WARNING,
                    "[grim-datastore] failed to load persisted check catalog for backend '"
                            + backendId + "' - falling back to process-local check names", e);
            CheckRegistry fallback = new CheckRegistry(new InMemoryCheckCatalogPersistence());
            fallback.reload();
            return fallback;
        }

        CheckCatalogPersistence persistence =
                new DataStoreCheckCatalogPersistence(initialRows, dataStore, logger);
        CheckRegistry registry = new CheckRegistry(persistence);
        registry.reload();
        return registry;
    }

    private @NotNull List<CheckCatalogRow> loadExistingCheckCatalogRows(
            @NotNull BackendV2 backend,
            @Nullable BackendConfig backendConfig) {
        DataSource dataSource = backend.unwrap(DataSource.class).orElse(null);
        if (dataSource != null) {
            return rowsFrom(new JdbcCheckCatalogPersistence(dataSource::getConnection, CHECKS_STORE).loadAll());
        }

        MongoDatabase mongo = backend.unwrap(MongoDatabase.class).orElse(null);
        if (mongo != null) {
            List<CheckCatalogRow> out = new ArrayList<>();
            for (Document d : mongo.getCollection(CHECKS_STORE).find()) {
                CheckCatalogRow row = checkCatalogRowFromDocument(d);
                if (row != null) out.add(row);
            }
            return out;
        }

        if (backendConfig instanceof RedisBackendConfig redisConfig) {
            List<CheckCatalogRow> rows = loadRedisCheckCatalogRows(backend, redisConfig.keyPrefix());
            if (rows != null) return rows;
        }

        logger.warning("[grim-datastore] no persisted check catalog loader available for v2 backend '"
                + backend.id() + "' - starting with an empty routed catalog view");
        return List.of();
    }

    private static @NotNull List<CheckCatalogRow> rowsFrom(@NotNull Iterable<CheckCatalogRow> rows) {
        List<CheckCatalogRow> out = new ArrayList<>();
        rows.forEach(out::add);
        return out;
    }

    private static @Nullable CheckCatalogRow checkCatalogRowFromDocument(@NotNull Document d) {
        Number id = d.get("check_id", Number.class);
        String stableKey = d.getString("stable_key");
        if (stableKey == null) {
            Object rawId = d.get("_id");
            if (rawId instanceof String s) stableKey = s;
        }
        if (id == null || stableKey == null) return null;
        Number introducedAt = d.get("introduced_at", Number.class);
        return new CheckCatalogRow(
                id.intValue(),
                stableKey,
                d.getString("display"),
                d.getString("description"),
                d.getString("introduced_version"),
                introducedAt == null ? 0L : introducedAt.longValue());
    }

    @SuppressWarnings("unchecked")
    private static @Nullable List<CheckCatalogRow> loadRedisCheckCatalogRows(
            @NotNull BackendV2 backend,
            @NotNull String keyPrefix) {
        try {
            Class<?> poolClass = Class.forName("redis.clients.jedis.JedisPool");
            Object pool = backend.unwrap((Class<Object>) poolClass).orElse(null);
            if (pool == null) return null;

            Class<?> scanParamsClass = Class.forName("redis.clients.jedis.params.ScanParams");
            Object params = scanParamsClass.getConstructor().newInstance();
            String rowPrefix = keyPrefix + CHECKS_STORE + ":";
            scanParamsClass.getMethod("match", String.class).invoke(params, rowPrefix + "*");
            scanParamsClass.getMethod("count", int.class).invoke(params, 1000);

            List<CheckCatalogRow> out = new ArrayList<>();
            Object jedis = poolClass.getMethod("getResource").invoke(pool);
            try {
                String cursor = "0";
                do {
                    Object result = jedis.getClass()
                            .getMethod("scan", String.class, scanParamsClass)
                            .invoke(jedis, cursor, params);
                    List<String> keys = (List<String>) result.getClass()
                            .getMethod("getResult")
                            .invoke(result);
                    for (String key : keys) {
                        if (key.startsWith(rowPrefix + "__idx:")) continue;
                        Map<String, String> hash = (Map<String, String>) jedis.getClass()
                                .getMethod("hgetAll", String.class)
                                .invoke(jedis, key);
                        CheckCatalogRow row = checkCatalogRowFromRedis(hash);
                        if (row != null) out.add(row);
                    }
                    cursor = (String) result.getClass().getMethod("getCursor").invoke(result);
                } while (!"0".equals(cursor));
            } finally {
                if (jedis instanceof AutoCloseable closeable) closeable.close();
            }
            return out;
        } catch (ClassNotFoundException e) {
            return null;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("failed to scan Redis check catalog", e);
        } catch (Exception e) {
            throw new IllegalStateException("failed to close Redis resource while scanning check catalog", e);
        }
    }

    private static @Nullable CheckCatalogRow checkCatalogRowFromRedis(@NotNull Map<String, String> h) {
        String id = h.get("check_id");
        String stableKey = h.get("stable_key");
        if (id == null || stableKey == null) return null;
        return new CheckCatalogRow(
                Integer.parseInt(id),
                stableKey,
                h.get("display"),
                h.get("description"),
                h.get("introduced_version"),
                parseLong(h.get("introduced_at")));
    }

    private static long parseLong(@Nullable String raw) {
        if (raw == null) return 0L;
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ignored) {
            return 0L;
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
            out.put(Categories.CHECK_CATALOG, new V2BackendBootstrap.Binding<>(
                    StoreId.grim(CHECKS_STORE), V2BuiltinKinds.checks()));
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
                    .withV2Startups(Categories.SERVER_STARTUP)
                    .withVerboseRegistry(verboseRegistry);
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
        long heartbeatMs = ownershipGate.enforced()
                ? config.ownership().renewIntervalMs()
                : instanceHeartbeatIntervalMs();
        long leaseTtlMs = config.ownership().leaseTtlMs();
        this.instanceId = loadPersistentInstanceId(plugin.getDataFolder().toPath());
        this.startupId = UUID.randomUUID();
        this.ownershipFence = UUID.randomUUID();
        this.startupStartedEpochMs = System.currentTimeMillis();

        this.instanceRegistry = V2InstanceRegistry.create(dataStore, dataStore.v2Routes(), logger);
        if (instanceRegistry == null) {
            String message = "[grim-datastore] v2 server startup registry route missing; "
                    + "startup history and crash recovery are disabled";
            logger.warning(message);
            if (ownershipGate.enforced()) {
                return ownershipDenied(message, null, -1L);
            }
            return null;
        }

        VerboseRegistry manifestRegistry = this.verboseRegistry;
        java.util.function.Supplier<byte[]> verboseManifest = () -> manifestRegistry == null
                ? VerboseManifest.textOnly(VerboseManifest.FLAVOR_V2_PUBLIC)
                : VerboseManifest.encode(
                        VerboseManifest.FLAVOR_V2_PUBLIC,
                        manifestRegistry.checkIdVersions(checkRegistry));
        ServerOwnershipMetadata metadata = new ServerOwnershipMetadata(
                config.serverName(),
                hostname(),
                GrimAPI.INSTANCE.getExternalAPI().getGrimVersion(),
                GrimAPI.INSTANCE.getPlatformServer().getPlatformImplementationString());

        boolean enforceOwnership = ownershipGate.enforced();
        OwnershipClaimResult ownershipClaim = null;
        if (enforceOwnership) {
            if (ownershipAdapter == null) {
                String message = "[grim-datastore] STORAGE DISABLED: backend for sessions does not expose "
                        + "a server ownership adapter, but persistent UUID ownership is enforced.";
                return ownershipDenied(message, null, -1L);
            }
            ownershipClaim = claimOwnershipWithOptionalWait(metadata);
            if (!ownershipClaim.claimed()) {
                ServerOwnershipSnapshot owner = ownershipClaim.currentOwner();
                long remaining = owner == null ? -1L
                        : Math.max(0L, owner.leaseExpiresAtEpochMs() - ownershipClaim.dbNowEpochMs());
                String message = "[grim-datastore] STORAGE DISABLED: live duplicate persistent storage UUID "
                        + instanceId + " detected. Existing startupId="
                        + (owner == null ? "unknown" : owner.ownerStartupId())
                        + " serverName='" + (owner == null ? "unknown" : owner.serverName()) + "'"
                        + " leaseRemainingMs=" + remaining
                        + "; this startupId=" + startupId
                        + ". A server data folder or storage-instance.uuid appears to be copied.";
                return ownershipDenied(message, owner == null ? null : owner.ownerStartupId(), remaining);
            }
            ownershipGate.open(startupId, ownershipFence, leaseTtlMs, config.ownership().safetyMarginMs());
        }

        long dbNow = ownershipClaim != null
                ? ownershipClaim.dbNowEpochMs()
                : dbNowBestEffort();
        V2InstanceRegistry.StartupClaim claim = instanceRegistry.openStartup(
                config.serverName(),
                instanceId,
                startupId,
                ownershipFence,
                startupStartedEpochMs,
                dbNow,
                metadata.hostname(),
                metadata.grimVersion(),
                metadata.serverVersionString(),
                verboseManifest.get());

        long recovered = recoverAfterStartupClaim(ownershipClaim, dbNow);
        if (recovered > 0) {
            logger.warning("[grim-datastore] recovered " + recovered
                    + " open session(s) after claiming storage startup");
        }

        if (enforceOwnership) {
            this.ownershipHeartbeatScheduler = new OwnershipHeartbeatScheduler(
                    ownershipAdapter,
                    OWNERSHIP_STORE,
                    startupId,
                    instanceId,
                    ownershipFence,
                    config.serverName(),
                    startupStartedEpochMs,
                    metadata.hostname(),
                    metadata.grimVersion(),
                    metadata.serverVersionString(),
                    verboseManifest,
                    leaseTtlMs,
                    config.ownership().safetyMarginMs(),
                    Duration.ofMillis(heartbeatMs),
                    ownershipGate,
                    instanceRegistry::publish,
                    this::disablePersistenceAfterLostOwnership,
                    logger);
            ownershipHeartbeatScheduler.start();
        } else {
            this.heartbeatScheduler = new HeartbeatScheduler(
                    startupId,
                    instanceId,
                    config.serverName(),
                    startupStartedEpochMs,
                    metadata.hostname(),
                    metadata.grimVersion(),
                    metadata.serverVersionString(),
                    verboseManifest,
                    Duration.ofMillis(heartbeatMs),
                    instanceRegistry::publish,
                    logger);
            heartbeatScheduler.start();
        }
        if (manifestRegistry != null) {
            if (ownershipHeartbeatScheduler != null) {
                manifestRegistry.onChange(ownershipHeartbeatScheduler::publishNowAndWait);
            } else if (heartbeatScheduler != null) {
                manifestRegistry.onChange(heartbeatScheduler::publishNowAndWait);
            }
        }
        startRecoverySweep();
        return claim;
    }

    private @NotNull OwnershipClaimResult claimOwnershipWithOptionalWait(
            @NotNull ServerOwnershipMetadata metadata) {
        long ttlMs = config.ownership().leaseTtlMs();
        OwnershipClaimResult first = claimOwnership(metadata, ttlMs);
        if (first.claimed()) return first;
        ServerOwnershipSnapshot owner = first.currentOwner();
        long remaining = owner == null ? 0L
                : Math.max(0L, owner.leaseExpiresAtEpochMs() - first.dbNowEpochMs());
        long waitMs = Math.min(config.ownership().startupWaitMs(), remaining + 50L);
        if (waitMs <= 0L || owner == null) return first;
        logger.warning("[grim-datastore] persistent storage UUID " + instanceId
                + " is owned by startupId=" + owner.ownerStartupId()
                + "; waiting " + waitMs + "ms for the ownership lease to expire before retrying");
        try {
            Thread.sleep(waitMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return first;
        }
        return claimOwnership(metadata, ttlMs);
    }

    private @NotNull OwnershipClaimResult claimOwnership(
            @NotNull ServerOwnershipMetadata metadata,
            long ttlMs) {
        try {
            return ownershipAdapter.claimOwnership(
                    OWNERSHIP_STORE, instanceId, startupId, ownershipFence, ttlMs, metadata);
        } catch (Exception e) {
            throw new RuntimeException("failed to claim server ownership", e);
        }
    }

    private @Nullable V2InstanceRegistry.StartupClaim ownershipDenied(
            @NotNull String message,
            @Nullable UUID conflictingStartupId,
            long leaseRemainingMs) {
        if (config.ownership().duplicatePersistentUuidAction() == DuplicatePersistentUuidAction.FAIL_STARTUP) {
            throw new FatalStorageStartupException(message);
        }
        logger.warning(message);
        ownershipGate.close("duplicate-persistent-uuid");
        return V2InstanceRegistry.StartupClaim.duplicate(
                startupId, instanceId,
                conflictingStartupId == null ? startupId : conflictingStartupId,
                leaseRemainingMs,
                message);
    }

    private void shutdownServerAfterFatalStorageStartup() {
        Runnable stop = () -> GrimAPI.INSTANCE.getPlatformServer().dispatchCommand(
                GrimAPI.INSTANCE.getPlatformServer().getConsoleSender(),
                "stop");
        try {
            GrimAPI.INSTANCE.getScheduler().getGlobalRegionScheduler().run(plugin, stop);
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "[grim-datastore] failed to schedule server shutdown after fatal storage startup", e);
            try {
                stop.run();
            } catch (RuntimeException immediateFailure) {
                logger.log(Level.SEVERE, "[grim-datastore] failed to dispatch stop command after fatal storage startup", immediateFailure);
            }
        }
    }

    private static final class FatalStorageStartupException extends RuntimeException {
        FatalStorageStartupException(@NotNull String message) {
            super(message);
        }
    }

    private long recoverAfterStartupClaim(
            @Nullable OwnershipClaimResult ownershipClaim,
            long dbNow) {
        long closed = 0L;
        if (ownershipClaim != null && ownershipClaim.previousOwner() != null) {
            ServerOwnershipSnapshot previous = ownershipClaim.previousOwner();
            if (!startupId.equals(previous.ownerStartupId())
                    && (previous.closedAtEpochMs() != ServerOwnershipSnapshot.OPEN
                    || previous.leaseExpiresAtEpochMs() <= dbNow)) {
                closed += instanceRegistry.recoverStartup(previous.ownerStartupId(), "expired-ownership");
            }
        }
        if (config.ownership().cleanupOtherServers()) {
            closed += instanceRegistry.recoverStaleStartups(
                    startupId, dbNow, config.ownership().staleStartupTtlMs());
        }
        return closed;
    }

    private long dbNowBestEffort() {
        if (ownershipAdapter != null) {
            try {
                return ownershipAdapter.dbNowEpochMs();
            } catch (Exception e) {
                logger.log(Level.FINE, "[grim-datastore] failed to read DB time; using local time", e);
            }
        }
        return System.currentTimeMillis();
    }

    private void startRecoverySweep() {
        if (!config.ownership().cleanupOtherServers() || instanceRegistry == null || startupId == null) return;
        stopRecoverySweep();
        recoverySweepExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "grim-storage-recovery-sweep");
            t.setDaemon(true);
            return t;
        });
        long intervalMs = config.ownership().recoverySweepIntervalMs();
        recoverySweepExecutor.scheduleAtFixedRate(
                this::runRecoverySweep,
                intervalMs,
                intervalMs,
                TimeUnit.MILLISECONDS);
    }

    private void runRecoverySweep() {
        V2InstanceRegistry registry = instanceRegistry;
        UUID currentStartup = startupId;
        if (registry == null || currentStartup == null) return;
        if (ownershipGate.enforced() && !ownershipGate.allowWrites()) return;
        try {
            long closed = registry.recoverStaleStartups(
                    currentStartup,
                    dbNowBestEffort(),
                    config.ownership().staleStartupTtlMs());
            if (closed > 0) {
                logger.warning("[grim-datastore] recovery sweep closed " + closed
                        + " open session(s) from stale startup rows");
            }
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "[grim-datastore] recovery sweep failed", e);
        }
    }

    private void stopRecoverySweep() {
        if (recoverySweepExecutor != null) {
            recoverySweepExecutor.shutdownNow();
            recoverySweepExecutor = null;
        }
    }

    private long instanceHeartbeatIntervalMs() {
        long configured = config.session().heartbeatIntervalMs();
        return configured > 0L ? configured : 30_000L;
    }

    private @NotNull VerboseRegistry buildVerboseRegistry() {
        VerboseCodecs.ensureRegistered();
        return new VerboseRegistryImpl(
                dataStore,
                checkRegistry,
                VerboseManifest.FLAVOR_V2_PUBLIC);
    }

    private void installLocalVerboseRegistry() {
        try {
            CheckRegistry localChecks = new CheckRegistry(new InMemoryCheckCatalogPersistence());
            localChecks.reload();
            this.checkRegistry = localChecks;
            this.verboseRegistry = buildVerboseRegistry();
        } catch (RuntimeException e) {
            logger.log(Level.WARNING,
                    "[grim-datastore] failed to initialise local verbose registry", e);
        }
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

    private void disableStorageAfterDuplicate() {
        logger.warning("[grim-datastore] storage disabled for this boot because another live Grim startup "
                + "is using this storage instance id. Runtime checks remain active.");
        if (heartbeatScheduler != null) {
            heartbeatScheduler.stop();
            heartbeatScheduler = null;
        }
        if (ownershipHeartbeatScheduler != null) {
            ownershipHeartbeatScheduler.stop();
            ownershipHeartbeatScheduler = null;
        }
        ownershipGate.close("duplicate-persistent-uuid");
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
        verboseRegistry = null;
        ownershipAdapter = null;
        ownershipFence = null;
        ownershipGate = ServerOwnershipGate.disabled();
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
                        + idClass + ", expected UUID-shaped binary -"
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
                        logger.warning("[grim-datastore] local-cache name resolver disabled; "
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
        if (sqliteBackend == null) return;
        if (config.migration().skip()) {
            logger.info("[grim-datastore] migration.skip=true; leaving legacy v0 un-migrated");
            return;
        }
        V0Sources.V0Source source = V0Sources.detect(
                dataFolder,
                GrimAPI.INSTANCE.getConfigManager().getConfig());
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

    @Override
    public void stop() {
        close();
    }

    public synchronized void reload() {
        logger.info("[grim-datastore] /grim reload: tearing down datastore...");
        close();
        start();
    }

    private synchronized void close() {
        stopDuplicateWarning();
        stopRecoverySweep();
        if (!enabled) return;
        stopHeartbeatSchedulersForShutdown();
        playerToggleStore.shutdown();
        if (violationSink != null) violationSink.shutDown();
        shutdownInstanceRegistry();
        if (dataStore != null && config != null) dataStore.flushAndClose(config.writePath().shutdownDrainTimeoutMs());
        closeOwnership("graceful");
        ownershipGate.close("shutdown");
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
        ownershipAdapter = null;
        ownershipFence = null;
        ownershipGate = ServerOwnershipGate.disabled();
        instanceId = null;
        startupId = null;
        startupStartedEpochMs = 0L;
        checkRegistry = null;
        config = null;
        loaded = false;
    }

    private void stopHeartbeatSchedulersForShutdown() {
        if (ownershipHeartbeatScheduler != null) {
            ownershipHeartbeatScheduler.publishNowAndWait();
            ownershipHeartbeatScheduler.stop();
            ownershipHeartbeatScheduler = null;
        }
        if (heartbeatScheduler != null) {
            heartbeatScheduler.publishNowAndWait();
            heartbeatScheduler.stop();
            heartbeatScheduler = null;
        }
    }

    private void shutdownInstanceRegistry() {
        if (instanceRegistry == null || startupId == null || config == null) return;
        if (ownershipGate.enforced() && !ownershipGate.allowWrites()) {
            logger.warning("[grim-datastore] skipping startup/session shutdown writes because DB ownership is no longer held");
            return;
        }
        long now = dbNowBestEffort();
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

    private void closeOwnership(@NotNull String reason) {
        if (ownershipAdapter == null || instanceId == null || startupId == null || ownershipFence == null) return;
        try {
            ownershipAdapter.closeOwnership(OWNERSHIP_STORE, instanceId, startupId, ownershipFence, reason);
        } catch (Exception e) {
            logger.log(Level.WARNING, "[grim-datastore] failed to close server ownership row", e);
        }
    }

    private synchronized void disablePersistenceAfterLostOwnership() {
        stopRecoverySweep();
        sessionTracker = SessionTracker.NOOP;
        liveWriteHooks = LiveWriteHooks.NOOP;
        playerToggleStore = PlayerToggleStore.NOOP;
        if (violationSink != null) {
            violationSink.shutDown();
            violationSink = null;
        }
    }

    public boolean isEnabled() { return enabled; }
    public boolean isLoaded() { return loaded; }

    public @Nullable DataStore dataStore() { return loaded ? dataStore : null; }
    public @Nullable HistoryService historyService() { return historyService; }
    public @Nullable NameResolver nameResolver() { return nameResolver; }
    public @Nullable ViolationSink violationSink() { return violationSink; }
    public @Nullable DataStoreConfig config() { return config; }
    public @Nullable VerboseRegistry verboseRegistry() { return verboseRegistry; }

    public @NotNull LiveWriteHooks liveWriteHooks() { return liveWriteHooks; }
    public @NotNull SessionTracker sessionTracker() { return sessionTracker; }
    public @NotNull PlayerToggleStore playerToggleStore() { return playerToggleStore; }

    @ApiStatus.Internal
    public @Nullable SqliteBackend sqliteBackendForCommands() {
        return null;
    }

    @ApiStatus.Internal
    public @Nullable CheckRegistry checkRegistryForCommands() {
        return checkRegistry;
    }

    @ApiStatus.Internal
    public @NotNull Map<String, Backend> allBackendsForCommands() {
        return Map.of();
    }

    private record SimpleContext(BackendConfig config, Logger logger, Path dataDirectory) implements BackendContext {}
}
