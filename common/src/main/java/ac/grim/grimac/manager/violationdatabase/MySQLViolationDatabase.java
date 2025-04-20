package ac.grim.grimac.manager.violationdatabase;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.player.GrimPlayer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class MySQLViolationDatabase implements ViolationDatabase {

    private final GrimPlugin plugin;
    private HikariDataSource dataSource;

    public MySQLViolationDatabase(GrimPlugin plugin, String url, String database, String username, String password) {
        this.plugin = plugin;
        setupDataSource(url, database, username, password);
    }

    private void setupDataSource(String url, String database, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + url + "/" + database);
        config.setUsername(username);
        config.setPassword(password);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setMaximumPoolSize(10);
        config.setAutoCommit(true);
        dataSource = new HikariDataSource(config);
    }

    @Override
    public void connect() {
        try (Connection connection = dataSource.getConnection()) {
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS violations(" +
                            "id INTEGER NOT NULL PRIMARY KEY AUTO_INCREMENT, " +
                            "server VARCHAR(255) NOT NULL, " +
                            "uuid CHAR(36) NOT NULL, " +
                            "check_name TEXT NOT NULL, " +
                            "verbose TEXT NOT NULL, " +
                            "vl INTEGER NOT NULL, " +
                            "created_at BIGINT NOT NULL" +
                            ")"
            ).execute();

            connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_violations_uuid ON violations(uuid);"
            ).execute();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to generate violations database:", ex);
        }
    }

    @Override
    public synchronized void logAlert(GrimPlayer player, String verbose, String checkName, int vls) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement insertAlert = connection.prepareStatement(
                     "INSERT INTO violations (server, uuid, check_name, verbose, vl, created_at) VALUES (?, ?, ?, ?, ?, ?)"
             )
        ) {
            insertAlert.setString(1, GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("history.server-name", "Prison"));
            insertAlert.setString(2, player.getUniqueId().toString());
            insertAlert.setString(3, checkName);
            insertAlert.setString(4, verbose);
            insertAlert.setInt(5, vls);
            insertAlert.setLong(6, System.currentTimeMillis());
            insertAlert.execute();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to log alert", ex);
        }
    }

    @Override
    public synchronized int getLogCount(UUID player) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement countLogs = connection.prepareStatement(
                     "SELECT COUNT(*) FROM violations WHERE uuid = ?"
             )
        ) {
            countLogs.setString(1, player.toString());
            ResultSet result = countLogs.executeQuery();
            if (result.next()) {
                return result.getInt(1);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to count logs", ex);
        }
        return 0;
    }

    @Override
    public synchronized List<Violation> getViolations(UUID player, int page, int limit) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement fetchLogs = connection.prepareStatement(
                     "SELECT server, uuid, check_name, verbose, vl, created_at FROM violations" +
                             " WHERE uuid = ? ORDER BY created_at DESC LIMIT ? OFFSET ?"
             )
        ) {
            fetchLogs.setString(1, player.toString());
            fetchLogs.setInt(2, limit);
            fetchLogs.setInt(3, (page - 1) * limit);
            return Violation.fromResultSet(fetchLogs.executeQuery());
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to fetch logs", ex);
            return null;
        }
    }

    @Override
    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public boolean sameConfig(String host, String db, String user, String pwd) {
        String wantUrl = "jdbc:mysql://" + host + "/" + db;
        return wantUrl.equalsIgnoreCase(dataSource.getJdbcUrl())
                && user.equals(dataSource.getUsername())
                && pwd .equals(dataSource.getPassword());   // Hikari stores clear text
    }
}
