package ac.grim.grimac.manager.violationdatabase;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.manager.init.ReloadableInitable;
import ac.grim.grimac.manager.init.start.StartableInitable;
import ac.grim.grimac.player.GrimPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class ViolationDatabaseManager implements StartableInitable, ReloadableInitable {

    private final GrimPlugin plugin;
    private boolean enabled = false;
    private boolean loaded = false;

    private @NonNull ViolationDatabase database;

    public ViolationDatabaseManager(GrimPlugin plugin) {
        this.plugin = plugin;
        this.database = NoOpViolationDatabase.INSTANCE;
    }

    @Override
    public void start() {
        load();
    }

    @Override
    public void reload() {
        load();
    }

    public void load() {
        ConfigManager cfg = GrimAPI.INSTANCE.getConfigManager().getConfig();
        this.enabled = cfg.getBooleanElse("history.enabled", false);
        String rawType = this.enabled ? cfg.getStringElse("history.database.type", "SQLITE").toUpperCase() : "NOOP";

        switch (rawType) {
            case "SQLITE" -> {
                if (!(database instanceof SQLiteViolationDatabase)) {
                    database.disconnect();
                    try {
                        // Init sqlite
                        Class.forName("org.sqlite.JDBC");
                        this.database = new SQLiteViolationDatabase(plugin);
                        database.connect();
                        loaded = true;
                    } catch (ClassNotFoundException e) {
                        plugin.getLogger().log(Level.SEVERE,
                                """
                                        Could not load SQLite driver for /grim history database.
                                        Download the minecraft-sqlite-jdbc mod/plugin for SQLite support, or change history.database.type
                                        Alternatively set history.enabled=false to remove this message if /grim history support is not desired"""
                        );
                        this.database = NoOpViolationDatabase.INSTANCE;
                        loaded = false;
                    } catch (SQLException e) {
                        this.database = NoOpViolationDatabase.INSTANCE;
                        loaded = false;
                    }
                }
            }

            case "MYSQL" -> {
                String host = cfg.getStringElse("history.database.host",     "localhost:3306");
                String db   = cfg.getStringElse("history.database.database", "grimac");
                String user = cfg.getStringElse("history.database.username", "root");
                String pwd  = cfg.getStringElse("history.database.password", "password");

                if (database instanceof MySQLViolationDatabase mysql
                        && mysql.sameConfig(host, db, user, pwd)) {
                    break;                          // nothing changed → keep pool
                }
                database.disconnect();
                database = new MySQLViolationDatabase(plugin, host, db, user, pwd);
                try {
                    database.connect();
                    loaded = true;
                } catch (SQLException e) {
                    this.database = NoOpViolationDatabase.INSTANCE;
                    loaded = false;
                }
            }

            default -> {                            // NOOP or invalid
                if (!(database instanceof NoOpViolationDatabase)) {
                    database.disconnect();
                    database = NoOpViolationDatabase.INSTANCE;
                    loaded = false;
                }
            }
        }
    }

    public void logAlert(GrimPlayer player, String verbose, String checkName, int vls) {
        GrimAPI.INSTANCE.getScheduler().getAsyncScheduler().runNow(plugin, () -> database.logAlert(player, verbose, checkName, vls));
    }

    public int getLogCount(UUID player) {
        return database.getLogCount(player);
    }

    public List<Violation> getViolations(UUID player, int page, int limit) {
        return database.getViolations(player, page, limit);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isLoaded() {
        return loaded;
    }
}
