package ac.grim.grimac.manager.violationdatabase.mysql;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.violationdatabase.*;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MySQLViolationDatabase implements ViolationDatabase {

    private HikariDataSource dataSource;
    private final DatabaseDialect dialect;

    public MySQLViolationDatabase(String url, String database, String username, String password) {
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
            createIndexIfNotExists(
                    DatabaseConstants.SERVERS_TABLE,
                    "idx_" + DatabaseConstants.SERVERS_TABLE + "_name",
                    DatabaseConstants.SERVERS_STRING_COLUMN);

            // 2. Create Lookup Table for Check Names
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + DatabaseConstants.CHECK_NAMES_TABLE + "(" +
                            "id " + pkSyntax + ", " +
                            DatabaseConstants.CHECK_NAMES_STRING_COLUMN + " VARCHAR(255) NOT NULL UNIQUE" +
                            ")"
            ).execute();
            createIndexIfNotExists(
                    DatabaseConstants.CHECK_NAMES_TABLE,
                    "idx_" + DatabaseConstants.CHECK_NAMES_TABLE + "_string",
                    DatabaseConstants.CHECK_NAMES_STRING_COLUMN);

            // --- NEW LOOKUP TABLES ---
            // 3. Create Lookup Table for Grim Versions
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + DatabaseConstants.GRIM_VERSIONS_TABLE + "(" +
                            "id " + pkSyntax + ", " +
                            DatabaseConstants.GRIM_VERSIONS_STRING_COLUMN + " VARCHAR(255) NOT NULL UNIQUE" +
                            ")"
            ).execute();
            createIndexIfNotExists(
                    DatabaseConstants.GRIM_VERSIONS_TABLE,
                    "idx_" + DatabaseConstants.GRIM_VERSIONS_TABLE + "_string",
                    DatabaseConstants.GRIM_VERSIONS_STRING_COLUMN);

            // 4. Create Lookup Table for Client Brands
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + DatabaseConstants.CLIENT_BRANDS_TABLE + "(" +
                            "id " + pkSyntax + ", " +
                            DatabaseConstants.CLIENT_BRANDS_STRING_COLUMN + " VARCHAR(255) NOT NULL UNIQUE" +
                            ")"
            ).execute();
            createIndexIfNotExists(
                    DatabaseConstants.CLIENT_BRANDS_TABLE,
                    "idx_" + DatabaseConstants.CLIENT_BRANDS_TABLE + "_string",
                    DatabaseConstants.CLIENT_BRANDS_STRING_COLUMN);

            // 5. Create Lookup Table for Client Versions
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + DatabaseConstants.CLIENT_VERSIONS_TABLE + "(" +
                            "id " + pkSyntax + ", " +
                            DatabaseConstants.CLIENT_VERSIONS_STRING_COLUMN + " VARCHAR(255) NOT NULL UNIQUE" +
                            ")"
            ).execute();
            createIndexIfNotExists(
                    DatabaseConstants.CLIENT_VERSIONS_TABLE,
                    "idx_" + DatabaseConstants.CLIENT_VERSIONS_TABLE + "_string",
                    DatabaseConstants.CLIENT_VERSIONS_STRING_COLUMN);

            // 6. Create Lookup Table for Server Versions
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + DatabaseConstants.SERVER_VERSIONS_TABLE + "(" +
                            "id " + pkSyntax + ", " +
                            DatabaseConstants.SERVER_VERSIONS_STRING_COLUMN + " VARCHAR(255) NOT NULL UNIQUE" +
                            ")"
            ).execute();
            createIndexIfNotExists(
                    DatabaseConstants.SERVER_VERSIONS_TABLE,
                    "idx_" + DatabaseConstants.SERVER_VERSIONS_TABLE + "_string",
                    DatabaseConstants.SERVER_VERSIONS_STRING_COLUMN);
            // --- END NEW LOOKUP TABLES ---

            // 7. Create Main Violations Table with ALL Foreign Keys and optimized UUID
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + DatabaseConstants.VIOLATIONS_TABLE + "(" +
                            "id " + pkSyntax + ", " +
                            DatabaseConstants.VIOLATIONS_SERVER_ID_COLUMN + " BIGINT NOT NULL, " +
                            DatabaseConstants.VIOLATIONS_UUID_COLUMN + " " + uuidType + " NOT NULL, " +
                            DatabaseConstants.VIOLATIONS_CHECK_NAME_ID_COLUMN + " BIGINT NOT NULL, " +
                            DatabaseConstants.VIOLATIONS_VERBOSE_COLUMN + " TEXT NOT NULL, " +
                            DatabaseConstants.VIOLATIONS_VL_COLUMN + " INT NOT NULL, " +
                            DatabaseConstants.VIOLATIONS_CREATED_AT_COLUMN + " BIGINT NOT NULL, " +
                            DatabaseConstants.VIOLATIONS_GRIM_VERSION_ID_COLUMN + " BIGINT NOT NULL, " + // NEW
                            DatabaseConstants.VIOLATIONS_CLIENT_BRAND_ID_COLUMN + " BIGINT NOT NULL, " + // NEW
                            DatabaseConstants.VIOLATIONS_CLIENT_VERSION_ID_COLUMN + " BIGINT NOT NULL, " + // NEW
                            DatabaseConstants.VIOLATIONS_SERVER_VERSION_ID_COLUMN + " BIGINT NOT NULL, " + // NEW
                            "FOREIGN KEY (" + DatabaseConstants.VIOLATIONS_SERVER_ID_COLUMN + ") REFERENCES " + DatabaseConstants.SERVERS_TABLE + "(id), " +
                            "FOREIGN KEY (" + DatabaseConstants.VIOLATIONS_CHECK_NAME_ID_COLUMN + ") REFERENCES " + DatabaseConstants.CHECK_NAMES_TABLE + "(id), " +
                            "FOREIGN KEY (" + DatabaseConstants.VIOLATIONS_GRIM_VERSION_ID_COLUMN + ") REFERENCES " + DatabaseConstants.GRIM_VERSIONS_TABLE + "(id), " + // NEW
                            "FOREIGN KEY (" + DatabaseConstants.VIOLATIONS_CLIENT_BRAND_ID_COLUMN + ") REFERENCES " + DatabaseConstants.CLIENT_BRANDS_TABLE + "(id), " + // NEW
                            "FOREIGN KEY (" + DatabaseConstants.VIOLATIONS_CLIENT_VERSION_ID_COLUMN + ") REFERENCES " + DatabaseConstants.CLIENT_VERSIONS_TABLE + "(id), " + // NEW
                            "FOREIGN KEY (" + DatabaseConstants.VIOLATIONS_SERVER_VERSION_ID_COLUMN + ") REFERENCES " + DatabaseConstants.SERVER_VERSIONS_TABLE + "(id)" + // NEW
                            ")"
            ).execute();

            // 8. Create Indexes for efficient querying on main table (includes new FKs)
            createIndexIfNotExists(
                    DatabaseConstants.VIOLATIONS_TABLE,
                    "idx_" + DatabaseConstants.VIOLATIONS_TABLE + "_uuid",
                    DatabaseConstants.VIOLATIONS_UUID_COLUMN);
            createIndexIfNotExists(
                    DatabaseConstants.VIOLATIONS_TABLE,
                    "idx_" + DatabaseConstants.VIOLATIONS_TABLE + "_created_at",
                    DatabaseConstants.VIOLATIONS_CREATED_AT_COLUMN);
            createIndexIfNotExists(
                    DatabaseConstants.VIOLATIONS_TABLE,
                    "idx_" + DatabaseConstants.VIOLATIONS_TABLE + "_server_id",
                    DatabaseConstants.VIOLATIONS_SERVER_ID_COLUMN);
            createIndexIfNotExists(
                    DatabaseConstants.VIOLATIONS_TABLE,
                    "idx_" + DatabaseConstants.VIOLATIONS_TABLE + "_check_name_id",
                    DatabaseConstants.VIOLATIONS_CHECK_NAME_ID_COLUMN);
            createIndexIfNotExists(
                    DatabaseConstants.VIOLATIONS_TABLE,
                    "idx_" + DatabaseConstants.VIOLATIONS_TABLE + "_grim_version_id",
                    DatabaseConstants.VIOLATIONS_GRIM_VERSION_ID_COLUMN);
            createIndexIfNotExists(
                    DatabaseConstants.VIOLATIONS_TABLE,
                    "idx_" + DatabaseConstants.VIOLATIONS_TABLE + "_client_brand_id",
                    DatabaseConstants.VIOLATIONS_CLIENT_BRAND_ID_COLUMN);
            createIndexIfNotExists(
                    DatabaseConstants.VIOLATIONS_TABLE,
                    "idx_" + DatabaseConstants.VIOLATIONS_TABLE + "_client_version_id",
                    DatabaseConstants.VIOLATIONS_CLIENT_VERSION_ID_COLUMN);
            createIndexIfNotExists(
                    DatabaseConstants.VIOLATIONS_TABLE,
                    "idx_" + DatabaseConstants.VIOLATIONS_TABLE + "_server_version_id",
                    DatabaseConstants.VIOLATIONS_SERVER_VERSION_ID_COLUMN);

            // 9. Create players info table and indexes
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + DatabaseConstants.PLAYERS_TABLE + "(" +
                            DatabaseConstants.PLAYERS_UUID_COLUMN + " " + uuidType + " NOT NULL PRIMARY KEY, " +
                            DatabaseConstants.PLAYERS_NAME_COLUMN + " VARCHAR(32) NOT NULL, " +
                            DatabaseConstants.PLAYERS_LAST_SEEN_COLUMN + " BIGINT NOT NULL DEFAULT (UNIX_TIMESTAMP() * 1000)" +
                            ")"
            ).execute();
            createIndexIfNotExists(
                    DatabaseConstants.PLAYERS_TABLE,
                    "idx_" + DatabaseConstants.PLAYERS_TABLE + "_uuid",
                    DatabaseConstants.PLAYERS_UUID_COLUMN);
            createIndexIfNotExists(
                    DatabaseConstants.PLAYERS_TABLE,
                    "idx_" + DatabaseConstants.PLAYERS_TABLE + "_name",
                    DatabaseConstants.PLAYERS_NAME_COLUMN);

        } catch (SQLException ex) {
            LogUtil.error("Failed to generate violations database:", ex);
            throw ex;
        }
    }

    @Override
    // Updated method signature to accept all new parameters
    public synchronized void logAlert(GrimPlayer player, String grimVersion, String verbose, String checkName, int vls) {
        try (Connection connection = dataSource.getConnection();
             // Updated INSERT statement with all new columns
             PreparedStatement insertAlert = connection.prepareStatement(
                     "INSERT INTO " + DatabaseConstants.VIOLATIONS_TABLE + " (" +
                             DatabaseConstants.VIOLATIONS_SERVER_ID_COLUMN + ", " +
                             DatabaseConstants.VIOLATIONS_UUID_COLUMN + ", " +
                             DatabaseConstants.VIOLATIONS_CHECK_NAME_ID_COLUMN + ", " +
                             DatabaseConstants.VIOLATIONS_VERBOSE_COLUMN + ", " +
                             DatabaseConstants.VIOLATIONS_VL_COLUMN + ", " +
                             DatabaseConstants.VIOLATIONS_CREATED_AT_COLUMN + ", " +
                             DatabaseConstants.VIOLATIONS_GRIM_VERSION_ID_COLUMN + ", " + // NEW
                             DatabaseConstants.VIOLATIONS_CLIENT_BRAND_ID_COLUMN + ", " + // NEW
                             DatabaseConstants.VIOLATIONS_CLIENT_VERSION_ID_COLUMN + ", " + // NEW
                             DatabaseConstants.VIOLATIONS_SERVER_VERSION_ID_COLUMN + // NEW
                             ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" // Total 10 parameters now
             )
        ) {
            // Get or create IDs for all deduplicated strings
            String serverName = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("history.server-name", "Prison");
            long serverId = DatabaseUtils.getOrCreateId(connection, dialect, DatabaseConstants.SERVERS_TABLE, DatabaseConstants.SERVERS_STRING_COLUMN, serverName);
            long checkNameId = DatabaseUtils.getOrCreateId(connection, dialect, DatabaseConstants.CHECK_NAMES_TABLE, DatabaseConstants.CHECK_NAMES_STRING_COLUMN, checkName);
            long grimVersionId = DatabaseUtils.getOrCreateId(connection, dialect, DatabaseConstants.GRIM_VERSIONS_TABLE, DatabaseConstants.GRIM_VERSIONS_STRING_COLUMN, grimVersion);
            long clientBrandId = DatabaseUtils.getOrCreateId(connection, dialect, DatabaseConstants.CLIENT_BRANDS_TABLE, DatabaseConstants.CLIENT_BRANDS_STRING_COLUMN, player.getBrand());
            long clientVersionId = DatabaseUtils.getOrCreateId(connection, dialect, DatabaseConstants.CLIENT_VERSIONS_TABLE, DatabaseConstants.CLIENT_VERSIONS_STRING_COLUMN, player.getClientVersion().getReleaseName());
            long serverVersionId = DatabaseUtils.getOrCreateId(connection, dialect, DatabaseConstants.SERVER_VERSIONS_TABLE, DatabaseConstants.SERVER_VERSIONS_STRING_COLUMN, PacketEvents.getAPI().getServerManager().getVersion().toString());

            // Set parameters for the PreparedStatement
            insertAlert.setLong(1, serverId);
            insertAlert.setBytes(2, DatabaseUtils.uuidToBytes(player.getUniqueId()));
            insertAlert.setLong(3, checkNameId);
            insertAlert.setString(4, verbose);
            insertAlert.setInt(5, vls);
            insertAlert.setLong(6, System.currentTimeMillis());
            insertAlert.setLong(7, grimVersionId); // NEW
            insertAlert.setLong(8, clientBrandId); // NEW
            insertAlert.setLong(9, clientVersionId); // NEW
            insertAlert.setLong(10, serverVersionId); // NEW

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
             // Updated SELECT statement with all new joins and column selections
             PreparedStatement fetchLogs = connection.prepareStatement(
                     "SELECT " +
                             "v." + DatabaseConstants.VIOLATIONS_ID_COLUMN + ", " +
                             "s." + DatabaseConstants.SERVERS_STRING_COLUMN + ", " +
                             "v." + DatabaseConstants.VIOLATIONS_UUID_COLUMN + ", " +
                             "cn." + DatabaseConstants.CHECK_NAMES_STRING_COLUMN + ", " +
                             "v." + DatabaseConstants.VIOLATIONS_VERBOSE_COLUMN + ", " +
                             "v." + DatabaseConstants.VIOLATIONS_VL_COLUMN + ", " +
                             "v." + DatabaseConstants.VIOLATIONS_CREATED_AT_COLUMN + ", " +
                             "gv." + DatabaseConstants.GRIM_VERSIONS_STRING_COLUMN + ", " + // NEW
                             "cb." + DatabaseConstants.CLIENT_BRANDS_STRING_COLUMN + ", " + // NEW
                             "clv." + DatabaseConstants.CLIENT_VERSIONS_STRING_COLUMN + ", " + // NEW
                             "srv." + DatabaseConstants.SERVER_VERSIONS_STRING_COLUMN + " " + // NEW
                             "FROM " + DatabaseConstants.VIOLATIONS_TABLE + " v " +
                             "JOIN " + DatabaseConstants.SERVERS_TABLE + " s ON v." + DatabaseConstants.VIOLATIONS_SERVER_ID_COLUMN + " = s.id " +
                             "JOIN " + DatabaseConstants.CHECK_NAMES_TABLE + " cn ON v." + DatabaseConstants.VIOLATIONS_CHECK_NAME_ID_COLUMN + " = cn.id " +
                             "JOIN " + DatabaseConstants.GRIM_VERSIONS_TABLE + " gv ON v." + DatabaseConstants.VIOLATIONS_GRIM_VERSION_ID_COLUMN + " = gv.id " + // NEW
                             "JOIN " + DatabaseConstants.CLIENT_BRANDS_TABLE + " cb ON v." + DatabaseConstants.VIOLATIONS_CLIENT_BRAND_ID_COLUMN + " = cb.id " + // NEW
                             "JOIN " + DatabaseConstants.CLIENT_VERSIONS_TABLE + " clv ON v." + DatabaseConstants.VIOLATIONS_CLIENT_VERSION_ID_COLUMN + " = clv.id " + // NEW
                             "JOIN " + DatabaseConstants.SERVER_VERSIONS_TABLE + " srv ON v." + DatabaseConstants.VIOLATIONS_SERVER_VERSION_ID_COLUMN + " = srv.id " + // NEW
                             "WHERE v." + DatabaseConstants.VIOLATIONS_UUID_COLUMN + " = ? ORDER BY v." + DatabaseConstants.VIOLATIONS_CREATED_AT_COLUMN + " DESC LIMIT ? OFFSET ?"
             )
        ) {
            fetchLogs.setBytes(1, DatabaseUtils.uuidToBytes(player));
            fetchLogs.setInt(2, limit);
            fetchLogs.setInt(3, (page - 1) * limit);
            return Violation.fromResultSet(fetchLogs.executeQuery(), true);
        } catch (SQLException ex) {
            LogUtil.error("Failed to fetch logs", ex);
            return null;
        }
    }

    @Override
    public void updateHistoryPlayer(GrimPlayer player) {
        try (Connection connection = dataSource.getConnection();
             // Updated SELECT statement with all new joins and column selections
             PreparedStatement updatePlayer = connection.prepareStatement(
                     "INSERT INTO " + DatabaseConstants.PLAYERS_TABLE + " (" +
                             DatabaseConstants.PLAYERS_UUID_COLUMN + ", " +
                             DatabaseConstants.PLAYERS_NAME_COLUMN + ", " +
                             DatabaseConstants.PLAYERS_LAST_SEEN_COLUMN + ") " +
                     "VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                             DatabaseConstants.PLAYERS_NAME_COLUMN + " = VALUES(" + DatabaseConstants.PLAYERS_NAME_COLUMN + "), " +
                             DatabaseConstants.PLAYERS_LAST_SEEN_COLUMN + " = VALUES(" + DatabaseConstants.PLAYERS_LAST_SEEN_COLUMN + ")")
        ) {
            updatePlayer.setBytes(1, DatabaseUtils.uuidToBytes(player.getUniqueId()));
            updatePlayer.setString(2, player.getName());
            updatePlayer.setLong(3, System.currentTimeMillis());

            updatePlayer.executeUpdate();
        } catch (SQLException ex) {
            LogUtil.error("Failed to update player history", ex);
        }
    }

    @Override
    public Optional<HistoryPlayer> getHistoryPlayer(UUID uuid) {
        return this.getHistoryPlayerByQuery(
                "WHERE " + DatabaseConstants.PLAYERS_UUID_COLUMN + " = ?",
                uuid);
    }

    @Override
    public Optional<HistoryPlayer> getHistoryPlayer(String playerName) {
        return this.getHistoryPlayerByQuery(
                "WHERE LOWER(" + DatabaseConstants.PLAYERS_NAME_COLUMN + ") = LOWER(?)",
                playerName);
    }

    private Optional<HistoryPlayer> getHistoryPlayerByQuery(String queryWhere, Object value) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement historyPlayer = connection.prepareStatement(
                     "SELECT " +
                             DatabaseConstants.PLAYERS_UUID_COLUMN + ", " +
                             DatabaseConstants.PLAYERS_NAME_COLUMN +
                             " FROM " + DatabaseConstants.PLAYERS_TABLE + " " +
                             queryWhere +
                             " ORDER BY " + DatabaseConstants.PLAYERS_LAST_SEEN_COLUMN + " DESC"
             )
        ) {
            if (value instanceof UUID uuid) {
                historyPlayer.setBytes(1, DatabaseUtils.uuidToBytes(uuid));
            } else if (value instanceof String stringValue) {
                historyPlayer.setString(1, stringValue);
            } else {
                historyPlayer.setObject(1, value);
            }

            ResultSet result = historyPlayer.executeQuery();
            if (result.next()) {
                byte[] uuidBytes = result.getBytes(DatabaseConstants.PLAYERS_UUID_COLUMN);
                String name = result.getString(DatabaseConstants.PLAYERS_NAME_COLUMN);
                return Optional.of(new HistoryPlayer(
                        DatabaseUtils.bytesToUuid(uuidBytes),
                        name
                ));
            }
        } catch (SQLException ex) {
            LogUtil.error("Failed to load player history", ex);
        }
        return Optional.empty();
    }

    private void createIndexIfNotExists(String tableName, String indexName, String columnName) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement check = connection.prepareStatement("SELECT COUNT(1) FROM information_schema.STATISTICS " +
                     "WHERE table_schema = DATABASE() " +
                     "AND table_name = ? " +
                     "AND index_name = ?")
        ) {
            check.setString(1, tableName);
            check.setString(2, indexName);

            try (ResultSet result = check.executeQuery()) {
                if (result.next() && result.getInt(1) == 0) {
                    connection.prepareStatement(
                            "CREATE INDEX " + indexName + " ON " + tableName + "(" + columnName + ")"
                    ).execute();
                }
            }
        } catch (SQLException ex) {
            LogUtil.error("Failed to create or update index", ex);
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
