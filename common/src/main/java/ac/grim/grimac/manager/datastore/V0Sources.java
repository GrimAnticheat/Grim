package ac.grim.grimac.manager.datastore;

import ac.grim.grimac.api.config.ConfigManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Builds a legacy migration source from either a plugin data folder (SQLite
 * file fallback) or the old {@code history.database.*} block in
 * {@code config.yml}. Shared by {@code DataStoreLifecycle.maybeMigrateLegacy}
 * at startup and by the {@code /grim history migrate} command at runtime so
 * both paths route sources identically.
 */
public final class V0Sources {

    private V0Sources() {}

    public record V0Source(String type, String jdbcUrl, @Nullable String username,
                           @Nullable String password, String summary) {}

    /**
     * Returns {@code null} when no usable legacy source is found — either the
     * on-disk SQLite file is absent (fresh install, or migration already run)
     * or {@code history.database.type} is set to {@code noop}.
     */
    public static @Nullable V0Source detect(@NotNull Path dataFolder, @NotNull ConfigManager cfg) {
        String rawType = cfg.getStringElse("history.database.type", "SQLITE")
                .toUpperCase(Locale.ROOT);
        return switch (rawType) {
            case "MYSQL" -> mysqlSource(cfg);
            case "POSTGRESQL" -> postgresqlSource(cfg);
            case "NOOP" -> null;
            default -> sqliteSource(dataFolder);
        };
    }

    public static @Nullable V0Source sqliteSource(@NotNull Path dataFolder) {
        Path legacy = dataFolder.resolve("violations.sqlite");
        if (!Files.isRegularFile(legacy)) {
            // Older builds dropped the file at <dataFolder>/data/violations.sqlite.
            // Fall back to that layout before giving up.
            Path fallback = dataFolder.resolve("data").resolve("violations.sqlite");
            if (!Files.isRegularFile(fallback)) return null;
            legacy = fallback;
        }
        return new V0Source(
                "sqlite",
                "jdbc:sqlite:" + legacy.toAbsolutePath(),
                null, null,
                "sqlite file " + legacy.getFileName());
    }

    private static V0Source mysqlSource(@NotNull ConfigManager cfg) {
        String host = cfg.getStringElse("history.database.host", "localhost");
        int port = cfg.getIntElse("history.database.port", 3306);
        String database = cfg.getStringElse("history.database.database", "grimac");
        String username = cfg.getStringElse("history.database.username", "root");
        String password = cfg.getStringElse("history.database.password", "");
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database;
        return new V0Source("mysql", url, username, password,
                "mysql " + host + ":" + port + "/" + database + " as " + username);
    }

    private static V0Source postgresqlSource(@NotNull ConfigManager cfg) {
        String host = cfg.getStringElse("history.database.host", "localhost");
        int port = cfg.getIntElse("history.database.port", 5432);
        String database = cfg.getStringElse("history.database.database", "grimac");
        String username = cfg.getStringElse("history.database.username", "postgres");
        String password = cfg.getStringElse("history.database.password", "");
        String url = "jdbc:postgresql://" + host + ":" + port + "/" + database;
        return new V0Source("postgresql", url, username, password,
                "postgresql " + host + ":" + port + "/" + database + " as " + username);
    }
}
