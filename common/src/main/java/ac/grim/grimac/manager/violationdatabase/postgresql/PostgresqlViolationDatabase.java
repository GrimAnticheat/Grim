package ac.grim.grimac.manager.violationdatabase.postgresql;

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
import java.util.UUID;

public class PostgresqlViolationDatabase implements ViolationDatabase {

    private static String quoteVerboseColumn() {
        return "\"" + DatabaseConstants.VIOLATIONS_VERBOSE_COLUMN + "\"";
    }

    private HikariDataSource dataSource;
    private final DatabaseDialect dialect;

    public PostgresqlViolationDatabase(String url, String database, String username, String password) {
        this.dialect = new PostgresqlDialect();
        setupDataSource(url, database, username, password);
    }

    private void setupDataSource(String url, String database, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://" + url + "/" + database);
        config.setUsername(username);
        config.setPassword(password);
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

            // --- NEW LOOKUP TABLES ---
            // 3. Create Lookup Table for Grim Versions
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + DatabaseConstants.GRIM_VERSIONS_TABLE + "(" +
                            "id " + pkSyntax + ", " +
                            DatabaseConstants.GRIM_VERSIONS_STRING_COLUMN + " VARCHAR(255) NOT NULL UNIQUE" +
                            ")"
            ).execute();
            connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_" + DatabaseConstants.GRIM_VERSIONS_TABLE + "_string ON " + DatabaseConstants.GRIM_VERSIONS_TABLE + "(" + DatabaseConstants.GRIM_VERSIONS_STRING_COLUMN + ");"
            ).execute();

            // 4. Create Lookup Table for Client Brands
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + DatabaseConstants.CLIENT_BRANDS_TABLE + "(" +
                            "id " + pkSyntax + ", " +
                            DatabaseConstants.CLIENT_BRANDS_STRING_COLUMN + " VARCHAR(255) NOT NULL UNIQUE" +
                            ")"
            ).execute();
            connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_" + DatabaseConstants.CLIENT_BRANDS_TABLE + "_string ON " + DatabaseConstants.CLIENT_BRANDS_TABLE + "(" + DatabaseConstants.CLIENT_BRANDS_STRING_COLUMN + ");"
            ).execute();

            // 5. Create Lookup Table for Client Versions
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + DatabaseConstants.CLIENT_VERSIONS_TABLE + "(" +
                            "id " + pkSyntax + ", " +
                            DatabaseConstants.CLIENT_VERSIONS_STRING_COLUMN + " VARCHAR(255) NOT NULL UNIQUE" +
                            ")"
            ).execute();
            connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_" + DatabaseConstants.CLIENT_VERSIONS_TABLE + "_string ON " + DatabaseConstants.CLIENT_VERSIONS_TABLE + "(" + DatabaseConstants.CLIENT_VERSIONS_STRING_COLUMN + ");"
            ).execute();

            // 6. Create Lookup Table for Server Versions
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + DatabaseConstants.SERVER_VERSIONS_TABLE + "(" +
                            "id " + pkSyntax + ", " +
                            DatabaseConstants.SERVER_VERSIONS_STRING_COLUMN + " VARCHAR(255) NOT NULL UNIQUE" +
                            ")"
            ).execute();
            connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_" + DatabaseConstants.SERVER_VERSIONS_TABLE + "_string ON " + DatabaseConstants.SERVER_VERSIONS_TABLE + "(" + DatabaseConstants.SERVER_VERSIONS_STRING_COLUMN + ");"
            ).execute();
            // --- END NEW LOOKUP TABLES ---

            // 7. Create Main Violations Table with ALL Foreign Keys and optimized UUID
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + DatabaseConstants.VIOLATIONS_TABLE + "(" +
                            "id " + pkSyntax + ", " +
                            DatabaseConstants.VIOLATIONS_SERVER_ID_COLUMN + " BIGINT NOT NULL, " +
                            DatabaseConstants.VIOLATIONS_UUID_COLUMN + " " + uuidType + " NOT NULL, " +
                            DatabaseConstants.VIOLATIONS_CHECK_NAME_ID_COLUMN + " BIGINT NOT NULL, " +
                            quoteVerboseColumn() + " TEXT NOT NULL, " +
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
                    "CREATE INDEX IF NOT EXISTS idx_" + DatabaseConstants.VIOLATIONS_TABLE + "_grim_version_id ON " + DatabaseConstants.VIOLATIONS_TABLE + "(" + DatabaseConstants.VIOLATIONS_GRIM_VERSION_ID_COLUMN + ");" // NEW
            ).execute();
            connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_" + DatabaseConstants.VIOLATIONS_TABLE + "_client_brand_id ON " + DatabaseConstants.VIOLATIONS_TABLE + "(" + DatabaseConstants.VIOLATIONS_CLIENT_BRAND_ID_COLUMN + ");" // NEW
            ).execute();
            connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_" + DatabaseConstants.VIOLATIONS_TABLE + "_client_version_id ON " + DatabaseConstants.VIOLATIONS_TABLE + "(" + DatabaseConstants.VIOLATIONS_CLIENT_VERSION_ID_COLUMN + ");" // NEW
            ).execute();
            connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_" + DatabaseConstants.VIOLATIONS_TABLE + "_server_version_id ON " + DatabaseConstants.VIOLATIONS_TABLE + "(" + DatabaseConstants.VIOLATIONS_SERVER_VERSION_ID_COLUMN + ");" // NEW
            ).execute();

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
                             quoteVerboseColumn() + ", " +
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
            insertAlert.setObject(2, player.getUniqueId());
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
    public synchronized int getLogCount(UUID uuid) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement countLogs = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + DatabaseConstants.VIOLATIONS_TABLE + " WHERE " + DatabaseConstants.VIOLATIONS_UUID_COLUMN + " = ?"
             )
        ) {
            countLogs.setObject(1, uuid);
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
    public synchronized List<Violation> getViolations(UUID uuid, int page, int limit) {
        try (Connection connection = dataSource.getConnection();
             // Updated SELECT statement with all new joins and column selections
             PreparedStatement fetchLogs = connection.prepareStatement(
                     "SELECT " +
                             "v." + DatabaseConstants.VIOLATIONS_ID_COLUMN + ", " +
                             "s." + DatabaseConstants.SERVERS_STRING_COLUMN + ", " +
                             "v." + DatabaseConstants.VIOLATIONS_UUID_COLUMN + ", " +
                             "cn." + DatabaseConstants.CHECK_NAMES_STRING_COLUMN + ", " +
                             "v." + quoteVerboseColumn() + ", " +
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
            fetchLogs.setObject(1, uuid);
            fetchLogs.setInt(2, limit);
            fetchLogs.setInt(3, Math.max(0, (page - 1)) * limit); // postgresql is not allowing negative numbers
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
        String wantUrl = "jdbc:postgresql://" + host + "/" + db;
        return wantUrl.equalsIgnoreCase(dataSource.getJdbcUrl())
                && user.equals(dataSource.getUsername())
                && pwd.equals(dataSource.getPassword());
    }
}
