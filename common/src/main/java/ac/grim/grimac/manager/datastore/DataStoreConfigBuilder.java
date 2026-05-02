package ac.grim.grimac.manager.datastore;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.storage.backend.BackendConfig;
import ac.grim.grimac.api.storage.backend.BackendConfigSource;
import ac.grim.grimac.api.storage.backend.BackendProvider;
import ac.grim.grimac.api.storage.backend.BackendRegistry;
import ac.grim.grimac.api.storage.category.Categories;
import ac.grim.grimac.api.storage.category.Category;
import ac.grim.grimac.api.storage.config.DataStoreConfig;
import ac.grim.grimac.api.storage.config.HistoryConfig;
import ac.grim.grimac.api.storage.config.MigrationConfig;
import ac.grim.grimac.api.storage.config.RetentionRule;
import ac.grim.grimac.api.storage.config.SessionConfig;
import ac.grim.grimac.api.storage.config.WaitStrategyType;
import ac.grim.grimac.api.storage.config.WritePathConfig;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Builds a {@link DataStoreConfig} from the shared {@link ConfigManager}.
 * All datastore keys live under namespace wrappers in their respective yml
 * files (database.yml uses {@code database:}, databases/&lt;id&gt;.yml uses
 * {@code &lt;id&gt;:}) so Configuralize's flat-merge doesn't collide with
 * config.yml / discord.yml / each other.
 *
 * <p>Per-backend settings are read via a {@link PrefixedSource} that
 * automatically applies the backend-id prefix — providers see ungranged
 * key names ({@code "host"}, {@code "port"}) and never need to know what
 * file they live in.
 */
public final class DataStoreConfigBuilder {

    private static final String NS = "database.";

    private final ConfigManager config;
    private final BackendRegistry registry;
    private final Path dataFolder;

    public DataStoreConfigBuilder(@NotNull BackendRegistry registry,
                                  @NotNull Path dataFolder,
                                  @NotNull ConfigManager config) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.dataFolder = Objects.requireNonNull(dataFolder, "dataFolder");
        this.config = Objects.requireNonNull(config, "config");
    }

    public boolean enabled() {
        return config.getBooleanElse(NS + "enabled", true);
    }

    public @NotNull DataStoreConfig build() {
        Map<Category<?>, String> routing = readRouting();
        Map<String, BackendConfig> backends = readBackends(routing);

        SessionConfig session = new SessionConfig(
                config.getLongElse(NS + "session.gap-ms", 600_000L),
                config.getBooleanElse(NS + "session.scope-per-server", true),
                config.getLongElse(NS + "session.heartbeat-interval-ms", 30_000L));

        WritePathConfig writePath = readWritePath();
        Map<Category<?>, RetentionRule> retention = readRetention();

        MigrationConfig migration = new MigrationConfig(
                config.getBooleanElse(NS + "migration.skip", false),
                config.getLongElse(NS + "migration.max-duration-ms", 0L));

        List<String> chain = config.getStringListElse(NS + "name-resolution.chain",
                List.of("local-cache", "offline-mode-uuid"));

        HistoryConfig history = new HistoryConfig(
                config.getIntElse(NS + "history.entries-per-page", 15),
                config.getLongElse(NS + "history.group-interval-ms", 30_000L));

        String serverName = config.getStringElse(NS + "server-name", "Unknown");

        return new DataStoreConfig(
                routing, backends, session, writePath, retention, migration, chain, history, serverName);
    }

    private Map<Category<?>, String> readRouting() {
        Map<String, Object> raw = config.getMapElse(NS + "routing", Map.of());
        Map<Category<?>, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : raw.entrySet()) {
            Category<?> cat = categoryFor(e.getKey());
            if (cat == null) continue; // unknown category id in yaml — ignore, warn elsewhere if needed
            out.put(cat, Objects.toString(e.getValue(), "none"));
        }
        return out;
    }

    private Map<String, BackendConfig> readBackends(Map<Category<?>, String> routing) {
        Map<String, BackendConfig> out = new LinkedHashMap<>();
        for (String backendId : routing.values()) {
            if (backendId.equals("none") || out.containsKey(backendId)) continue;
            BackendProvider provider = registry.lookup(backendId);
            if (provider == null) {
                throw new IllegalArgumentException("no backend provider registered for id '" + backendId
                        + "' referenced in routing (registered: " + registry.registeredIds() + ")");
            }
            out.put(backendId, provider.readConfig(new PrefixedSource(config, backendId)));
        }
        return out;
    }

    private WritePathConfig readWritePath() {
        int capacity = config.getIntElse(NS + "write-path.queue-capacity", 16384);
        if (capacity <= 0 || Integer.bitCount(capacity) != 1) {
            throw new IllegalArgumentException(
                    "database.write-path.queue-capacity must be a positive power of two (got " + capacity
                            + "). Example values: 4096, 8192, 16384, 32768.");
        }
        String waitRaw = config.getStringElse(NS + "write-path.wait-strategy", "BLOCKING");
        WaitStrategyType wait;
        try {
            wait = WaitStrategyType.valueOf(waitRaw.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "database.write-path.wait-strategy must be one of BLOCKING, TIMEOUT_BLOCKING, SLEEPING, "
                            + "YIELDING, BUSY_SPIN (got '" + waitRaw + "')");
        }
        return new WritePathConfig(
                capacity,
                config.getIntElse(NS + "write-path.batch-size", 256),
                config.getLongElse(NS + "write-path.flush-interval-ms", 1000L),
                config.getLongElse(NS + "write-path.warn-rate-ms", 10_000L),
                config.getLongElse(NS + "write-path.shutdown-drain-timeout-ms", 5000L),
                wait);
    }

    private Map<Category<?>, RetentionRule> readRetention() {
        Map<String, Object> raw = config.getMapElse(NS + "retention", Map.of());
        Map<Category<?>, RetentionRule> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : raw.entrySet()) {
            Category<?> cat = categoryFor(e.getKey());
            if (cat == null) continue; // unknown category id in yaml — nothing to retain under it
            String base = NS + "retention." + e.getKey() + ".";
            boolean enabled = config.getBooleanElse(base + "enabled", false);
            long days = config.getLongElse(base + "max-age-days", 0L);
            out.put(cat, new RetentionRule(enabled, days));
        }
        return out;
    }

    private static Category<?> categoryFor(String id) {
        return switch (id) {
            case "violation" -> Categories.VIOLATION;
            case "session" -> Categories.SESSION;
            case "player-identity" -> Categories.PLAYER_IDENTITY;
            case "setting" -> Categories.SETTING;
            case "blob" -> Categories.BLOB;
            default -> null;
        };
    }

    /**
     * Adapts the shared ConfigManager into a per-backend
     * {@link BackendConfigSource} by prepending the backend-id prefix on
     * every read. So a MySQL provider reading {@code "host"} gets
     * {@code "mysql.host"} from the shared config.
     */
    private static final class PrefixedSource implements BackendConfigSource {
        private final ConfigManager delegate;
        private final String prefix;

        PrefixedSource(ConfigManager delegate, String backendId) {
            this.delegate = delegate;
            this.prefix = backendId + ".";
        }

        @Override public @NotNull String getString(@NotNull String key, @NotNull String defaultValue) {
            return delegate.getStringElse(prefix + key, defaultValue);
        }
        @Override public int getInt(@NotNull String key, int defaultValue) {
            return delegate.getIntElse(prefix + key, defaultValue);
        }
        @Override public long getLong(@NotNull String key, long defaultValue) {
            return delegate.getLongElse(prefix + key, defaultValue);
        }
        @Override public boolean getBoolean(@NotNull String key, boolean defaultValue) {
            return delegate.getBooleanElse(prefix + key, defaultValue);
        }
        @Override public @NotNull List<String> getStringList(@NotNull String key, @NotNull List<String> defaultValue) {
            return delegate.getStringListElse(prefix + key, defaultValue);
        }
    }
}
