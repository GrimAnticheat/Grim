package ac.grim.grimac.manager.violationdatabase.mysql;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.manager.violationdatabase.DatabaseConstants;
import ac.grim.grimac.manager.violationdatabase.DatabaseDialect;
import ac.grim.grimac.manager.violationdatabase.DatabaseUtils;
import ac.grim.grimac.manager.violationdatabase.Violation;
import ac.grim.grimac.manager.violationdatabase.ViolationDatabase;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.List;
import java.util.UUID;

public class MySQLViolationDatabase implements ViolationDatabase {

    private final GrimPlugin plugin;
    private HikariDataSource dataSource;
    private final DatabaseDialect dialect;

    public MySQLViolationDatabase(GrimPlugin plugin, String url, String database, String username, String password) {
        this.plugin = plugin;
        this.dialect = new MySQLDialect();
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
    public void connect() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String pkSyntax = dialect.getAutoIncrementPrimaryKeySyntax();
            String uuidType = dialect.getUuidColumnType();

            // 1. Create Lookup Table for Server Names
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + DatabaseConstants.SERVERS_TABLE + "(" +
                            "id " + pkSyntax + ", " +
                            DatabaseConstants.SERVERS_STRING_COLUMN + " VARCHAR(255) NOT NULL UNIQUE" +
                            ")"
            ).execute();
            connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_" + DatabaseConstants.SERVERS_TABLE + "_name ON " + DatabaseConstants.SERVERS_TABLE + "(" + DatabaseConstants.SERVERS_STRING_COLUMN + ");"
            ).execute();

            // 2. Create Lookup Table for Check Names
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + DatabaseConstants.CHECK_NAMES_TABLE + "(" +
                            "id " + pkSyntax + ", " +
                            DatabaseConstants.CHECK_NAMES_STRING_COLUMN + " VARCHAR(255) NOT NULL UNIQUE" +
                            ")"
            ).execute();
            connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_" + DatabaseConstants.CHECK_NAMES_TABLE + "_string ON " + DatabaseConstants.CHECK_NAMES_TABLE + "(" + DatabaseConstants.CHECK_NAMES_STRING_COLUMN + ");"
            ).execute();

            // 3. Create Lookup Table for Version Strings
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + DatabaseConstants.VERSIONS_TABLE + "(" +
                            "id " + pkSyntax + ", " +
                            DatabaseConstants.VERSIONS_STRING_COLUMN + " VARCHAR(255) NOT NULL UNIQUE" +
                            ")"
            ).execute();
            connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_" + DatabaseConstants.VERSIONS_TABLE + "_string ON " + DatabaseConstants.VERSIONS_TABLE + "(" + DatabaseConstants.VERSIONS_STRING_COLUMN + ");"
            ).execute();

            // 4. Create Main Violations Table with Foreign Keys and optimized UUID
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + DatabaseConstants.VIOLATIONS_TABLE + "(" +
                            "id " + pkSyntax + ", " +
                            DatabaseConstants.VIOLATIONS_SERVER_ID_COLUMN + " BIGINT NOT NULL, " +
                            DatabaseConstants.VIOLATIONS_UUID_COLUMN + " " + uuidType + " NOT NULL, " +
                            DatabaseConstants.VIOLATIONS_CHECK_NAME_ID_COLUMN + " BIGINT NOT NULL, " +
                            DatabaseConstants.VIOLATIONS_VERBOSE_COLUMN + " TEXT NOT NULL, " +
                            DatabaseConstants.VIOLATIONS_VL_COLUMN + " INT NOT NULL, " +
                            DatabaseConstants.VIOLATIONS_CREATED_AT_COLUMN + " BIGINT NOT NULL, " +
                            DatabaseConstants.VIOLATIONS_VERSION_ID_COLUMN + " BIGINT NOT NULL, " +
                            "FOREIGN KEY (" + DatabaseConstants.VIOLATIONS_SERVER_ID_COLUMN + ") REFERENCES " + DatabaseConstants.SERVERS_TABLE + "(id), " +
                            "FOREIGN KEY (" + DatabaseConstants.VIOLATIONS_CHECK_NAME_ID_COLUMN + ") REFERENCES " + DatabaseConstants.CHECK_NAMES_TABLE + "(id), " +
                            "FOREIGN KEY (" + DatabaseConstants.VIOLATIONS_VERSION_ID_COLUMN + ") REFERENCES " + DatabaseConstants.VERSIONS_TABLE + "(id)" +
                            ")"
            ).execute();

            // 5. Create Indexes for efficient querying on main table
            connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_" + DatabaseConstants.VIOLATIONS_TABLE + "_uuid ON " + DatabaseConstants.VIOLATIONS_TABLE + "(" + DatabaseConstants.VIOLATIONS_UUID_COLUMN + ");"
            ).execute();
            connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_" + DatabaseConstants.VIOLATIONS_TABLE + "_created_at ON " + DatabaseConstants.VIOLATIONS_TABLE + "(" + DatabaseConstants.VIOLATIONS_CREATED_AT_COLUMN + ");"
            ).execute();
            connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_" + DatabaseConstants.VIOLATIONS_TABLE + "_server_id ON " + DatabaseConstants.VIOLATIONS_TABLE + "(" + DatabaseConstants.VIOLATIONS_SERVER_ID_COLUMN + ");"
            ).execute();
            connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_" + DatabaseConstants.VIOLATIONS_TABLE + "_check_name_id ON " + DatabaseConstants.VIOLATIONS_TABLE + "(" + DatabaseConstants.VIOLATIONS_CHECK_NAME_ID_COLUMN + ");"
            ).execute();
            connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_" + DatabaseConstants.VIOLATIONS_TABLE + "_version_id ON " + DatabaseConstants.VIOLATIONS_TABLE + "(" + DatabaseConstants.VIOLATIONS_VERSION_ID_COLUMN + ");"
            ).execute();

        } catch (SQLException ex) {
            LogUtil.error("Failed to generate violations database:", ex);
            throw ex;
        }
    }

    @Override
    public synchronized void logAlert(GrimPlayer player, String grimVersion, String verbose, String checkName, int vls) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement insertAlert = connection.prepareStatement(
                     "INSERT INTO " + DatabaseConstants.VIOLATIONS_TABLE + " (" +
                             DatabaseConstants.VIOLATIONS_SERVER_ID_COLUMN + ", " +
                             DatabaseConstants.VIOLATIONS_UUID_COLUMN + ", " +
                             DatabaseConstants.VIOLATIONS_CHECK_NAME_ID_COLUMN + ", " +
                             DatabaseConstants.VIOLATIONS_VERBOSE_COLUMN + ", " +
                             DatabaseConstants.VIOLATIONS_VL_COLUMN + ", " +
                             DatabaseConstants.VIOLATIONS_CREATED_AT_COLUMN + ", " +
                             DatabaseConstants.VIOLATIONS_VERSION_ID_COLUMN +
                             ") VALUES (?, ?, ?, ?, ?, ?, ?)"
             )
        ) {
            String serverName = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("history.server-name", "Prison");
            long serverId = DatabaseUtils.getOrCreateId(connection, dialect, DatabaseConstants.SERVERS_TABLE, DatabaseConstants.SERVERS_STRING_COLUMN, serverName);
            long checkNameId = DatabaseUtils.getOrCreateId(connection, dialect, DatabaseConstants.CHECK_NAMES_TABLE, DatabaseConstants.CHECK_NAMES_STRING_COLUMN, checkName);
            long versionId = DatabaseUtils.getOrCreateId(connection, dialect, DatabaseConstants.VERSIONS_TABLE, DatabaseConstants.VERSIONS_STRING_COLUMN, grimVersion);

            insertAlert.setLong(1, serverId);
            insertAlert.setBytes(2, DatabaseUtils.uuidToBytes(player.getUniqueId()));
            insertAlert.setLong(3, checkNameId);
            insertAlert.setString(4, verbose);
            insertAlert.setInt(5, vls);
            insertAlert.setLong(6, System.currentTimeMillis());
            insertAlert.setLong(7, versionId);

            insertAlert.execute();
        } catch (SQLException ex) {
            LogUtil.error("Failed to log alert", ex);
        }
    }

    @Override
    public synchronized int getLogCount(UUID player) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement countLogs = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + DatabaseConstants.VIOLATIONS_TABLE + " WHERE " + DatabaseConstants.VIOLATIONS_UUID_COLUMN + " = ?"
             )
        ) {
            countLogs.setBytes(1, DatabaseUtils.uuidToBytes(player));
            ResultSet result = countLogs.executeQuery();
            if (result.next()) {
                return result.getInt(1);
            }
        } catch (SQLException ex) {
            LogUtil.error("Failed to count logs", ex);
        }
        return 0;
    }

    @Override
    public synchronized List<Violation> getViolations(UUID player, int page, int limit) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement fetchLogs = connection.prepareStatement(
                     "SELECT " +
                             "v." + DatabaseConstants.VIOLATIONS_ID_COLUMN + ", " +
                             "s." + DatabaseConstants.SERVERS_STRING_COLUMN + ", " +
                             "v." + DatabaseConstants.VIOLATIONS_UUID_COLUMN + ", " +
                             "cn." + DatabaseConstants.CHECK_NAMES_STRING_COLUMN + ", " +
                             "v." + DatabaseConstants.VIOLATIONS_VERBOSE_COLUMN + ", " +
                             "v." + DatabaseConstants.VIOLATIONS_VL_COLUMN + ", " +
                             "v." + DatabaseConstants.VIOLATIONS_CREATED_AT_COLUMN + ", " +
                             "vers." + DatabaseConstants.VERSIONS_STRING_COLUMN + " " +
                             "FROM " + DatabaseConstants.VIOLATIONS_TABLE + " v " +
                             "JOIN " + DatabaseConstants.SERVERS_TABLE + " s ON v." + DatabaseConstants.VIOLATIONS_SERVER_ID_COLUMN + " = s.id " +
                             "JOIN " + DatabaseConstants.CHECK_NAMES_TABLE + " cn ON v." + DatabaseConstants.VIOLATIONS_CHECK_NAME_ID_COLUMN + " = cn.id " +
                             "JOIN " + DatabaseConstants.VERSIONS_TABLE + " vers ON v." + DatabaseConstants.VIOLATIONS_VERSION_ID_COLUMN + " = vers.id " +
                             "WHERE v." + DatabaseConstants.VIOLATIONS_UUID_COLUMN + " = ? ORDER BY v." + DatabaseConstants.VIOLATIONS_CREATED_AT_COLUMN + " DESC LIMIT ? OFFSET ?"
             )
        ) {
            fetchLogs.setBytes(1, DatabaseUtils.uuidToBytes(player));
            fetchLogs.setInt(2, limit);
            fetchLogs.setInt(3, (page - 1) * limit);
            return Violation.fromResultSet(fetchLogs.executeQuery());
        } catch (SQLException ex) {
            LogUtil.error("Failed to fetch logs", ex);
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
                && pwd .equals(dataSource.getPassword());
    }
}
