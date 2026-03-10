package ac.grim.grimac.manager.violationdatabase;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.manager.init.ReloadableInitable;
import ac.grim.grimac.manager.init.start.StartableInitable;
import ac.grim.grimac.manager.violationdatabase.mysql.MySQLViolationDatabase;
import ac.grim.grimac.manager.violationdatabase.postgresql.PostgresqlViolationDatabase;
import ac.grim.grimac.manager.violationdatabase.sqlite.SQLiteViolationDatabase;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class ViolationDatabaseManager implements StartableInitable, ReloadableInitable {

    private final GrimPlugin plugin;
    @Getter private boolean enabled = false;
    @Getter private boolean loaded = false;

    private @NotNull ViolationDatabase database;

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
                        LogUtil.error(
                                """
                                        IMPORTANT: Could not load SQLite driver for /grim history database.
                                        Download the minecraft-sqlite-jdbc mod/plugin for SQLite support, or change history.database.type
                                        Alternatively set history.enabled=false to remove this message if /grim history support is not desired"""
                        );
                        this.database = NoOpViolationDatabase.INSTANCE;
                        loaded = false;
                    } catch (SQLException e) {
                        LogUtil.error(e);
                        this.database = NoOpViolationDatabase.INSTANCE;
                        loaded = false;
                    }
                }
            }

            case "MYSQL" -> {
                int port = cfg.getIntElse("history.database.port", 3306);
                String host = cfg.getStringElse("history.database.host", "localhost") + ":" + port;
                String db = cfg.getStringElse("history.database.database", "grimac");
                String user = cfg.getStringElse("history.database.username", "root");
                String pwd = cfg.getStringElse("history.database.password", "password");

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
                    LogUtil.error(e);
                    this.database = NoOpViolationDatabase.INSTANCE;
                    loaded = false;
                }
            }

            case "POSTGRESQL" -> {
                int port = cfg.getIntElse("history.database.port", 3306);
                String host = cfg.getStringElse("history.database.host", "localhost") + ":" + port;
                String db   = cfg.getStringElse("history.database.database", "grimac");
                String user = cfg.getStringElse("history.database.username", "root");
                String pwd  = cfg.getStringElse("history.database.password", "password");

                if (database instanceof PostgresqlViolationDatabase postgresql
                        && postgresql.sameConfig(host, db, user, pwd)) {
                    break;                          // nothing changed → keep pool
                }
                database.disconnect();
                database = new PostgresqlViolationDatabase(host, db, user, pwd);
                try {
                    database.connect();
                    loaded = true;
                } catch (SQLException e) {
                    LogUtil.error(e);
                    this.database = NoOpViolationDatabase.INSTANCE;
                    loaded = false;
                }
            }

            default -> { // NOOP or invalid
                if (!(database instanceof NoOpViolationDatabase)) {
                    database.disconnect();
                    database = NoOpViolationDatabase.INSTANCE;
                    loaded = false;
                }
            }
        }
    }

    public void logAlert(GrimPlayer player, String verbose, String checkName, int vls) {
        String grimVersion = GrimAPI.INSTANCE.getExternalAPI().getGrimVersion();
        GrimAPI.INSTANCE.getScheduler().getAsyncScheduler().runNow(plugin, () -> database.logAlert(player, grimVersion, verbose, checkName, vls));
    }

    public int getLogCount(UUID player) {
        return database.getLogCount(player);
    }

    public List<Violation> getViolations(UUID player, int page, int limit) {
        return database.getViolations(player, page, limit);
    }
}
