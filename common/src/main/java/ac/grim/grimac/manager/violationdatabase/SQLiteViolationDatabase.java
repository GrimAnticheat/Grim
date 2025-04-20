package ac.grim.grimac.manager.violationdatabase;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.player.GrimPlayer;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class SQLiteViolationDatabase implements ViolationDatabase {

    private final GrimPlugin plugin;

    private Connection openConnection;

    public SQLiteViolationDatabase(@NotNull GrimPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void connect() {
        try (Connection connection = getConnection()) {
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS violations(" +
                            "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                            "server VARCHAR(255) NOT NULL, " +
                            "uuid CHARACTER(36) NOT NULL, " +
                            "check_name TEXT NOT NULL, " +
                            "verbose TEXT NOT NULL, " +
                            "vl INTEGER NOT NULL, " +
                            "created_at BIGINT NOT NULL" +
                            ")"
            ).execute();

            connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_violations_uuid ON violations(uuid)"
            ).execute();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to generate violations database:", ex);
        }
    }

    @Override
    public synchronized void logAlert(GrimPlayer player, String verbose, String checkName, int vls) {
        try (
                Connection connection = getConnection();
                PreparedStatement insertLog = connection.prepareStatement(
                        "INSERT INTO violations (server, uuid, check_name, verbose, vl, created_at) VALUES (?, ?, ?, ?, ?, ?)"
                )
        ) {
            insertLog.setString(1, GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("history.server-name", "Prison"));
            insertLog.setString(2, player.getUniqueId().toString());
            insertLog.setString(3, verbose);
            insertLog.setString(4, checkName);
            insertLog.setInt(5, vls);
            insertLog.setLong(6, System.currentTimeMillis());

            insertLog.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to insert violation:", ex);
        }
    }

    public synchronized int getLogCount(UUID player) {
        try (
                Connection connection = getConnection();
                PreparedStatement fetchLogs = connection.prepareStatement(
                        "SELECT COUNT(*) FROM violations WHERE uuid = ?"
                )
        ) {
            fetchLogs.setString(1, player.toString());
            ResultSet resultSet = fetchLogs.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to fetch number of violations:", ex);
        }
        return 0;
    }

    @Override
    public synchronized List<Violation> getViolations(UUID player, int page, int limit) {
        List<Violation> violations = new ArrayList<>();
        try (
                Connection connection = getConnection();
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
            plugin.getLogger().log(Level.SEVERE, "Failed to fetch violations:", ex);
        }

        return violations;
    }

    @Override
    public void disconnect() {
        try {
            if (openConnection != null && !openConnection.isClosed()) {
                openConnection.close();
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to close connection", ex);
        }
    }

    protected synchronized Connection getConnection() throws SQLException {
        if (openConnection == null || openConnection.isClosed()) {
            openConnection = openConnection();
        }
        return openConnection;
    }

    protected Connection openConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + File.separator + "violations.sqlite");
    }
}
