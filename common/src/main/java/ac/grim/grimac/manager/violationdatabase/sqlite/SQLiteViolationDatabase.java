package ac.grim.grimac.manager.violationdatabase.sqlite;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.manager.violationdatabase.DatabaseConstants;
import ac.grim.grimac.manager.violationdatabase.DatabaseDialect;
import ac.grim.grimac.manager.violationdatabase.DatabaseUtils;
import ac.grim.grimac.manager.violationdatabase.Violation;
import ac.grim.grimac.manager.violationdatabase.ViolationDatabase;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;

import com.github.retrooper.packetevents.PacketEvents;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SQLiteViolationDatabase implements ViolationDatabase {

    private final GrimPlugin plugin;
    private Connection openConnection;
    private final DatabaseDialect dialect;

    public SQLiteViolationDatabase(@NotNull GrimPlugin plugin) {
        this.plugin = plugin;
        this.dialect = new SQLiteDialect();
    }

    @Override
    public void connect() throws SQLException {
        try (Connection connection = getConnection()) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }

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
                    "CREATE INDEX IF NOT EXISTS idx_" + DatabaseConstants.SERVERS_TABLE + "_name ON " + DatabaseConstants.SERVERS_TABLE + "(" + DatabaseConstants.SERVERS_STRING_COLUMN + ")"
            ).execute();

            // 2. Create Lookup Table for Check Names
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + DatabaseConstants.CHECK_NAMES_TABLE + "(" +
                            "id " + pkSyntax + ", " +
                            DatabaseConstants.CHECK_NAMES_STRING_COLUMN + " VARCHAR(255) NOT NULL UNIQUE" +
                            ")"
            ).execute();
            connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_" + DatabaseConstants.CHECK_NAMES_TABLE + "_string ON " + DatabaseConstants.CHECK_NAMES_TABLE + "(" + DatabaseConstants.CHECK_NAMES_STRING_COLUMN + ")"
            ).execute();

            // 3. Create Lookup Table for Grim Versions (was VERSIONS_TABLE)
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + DatabaseConstants.GRIM_VERSIONS_TABLE + "(" +
                            "id " + pkSyntax + ", " +
                            DatabaseConstants.GRIM_VERSIONS_STRING_COLUMN + " VARCHAR(255) NOT NULL UNIQUE" +
                            ")"
            ).execute();
            connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_" + DatabaseConstants.GRIM_VERSIONS_TABLE + "_string ON " + DatabaseConstants.GRIM_VERSIONS_TABLE + "(" + DatabaseConstants.GRIM_VERSIONS_STRING_COLUMN + ")"
            ).execute();

            // 4. Create Lookup Table for Client Brands (NEW)
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + DatabaseConstants.CLIENT_BRANDS_TABLE + "(" +
                            "id " + pkSyntax + ", " +
                            DatabaseConstants.CLIENT_BRANDS_STRING_COLUMN + " VARCHAR(255) NOT NULL UNIQUE" +
                            ")"
            ).execute();
            connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_" + DatabaseConstants.CLIENT_BRANDS_TABLE + "_string ON " + DatabaseConstants.CLIENT_BRANDS_TABLE + "(" + DatabaseConstants.CLIENT_BRANDS_STRING_COLUMN + ")"
            ).execute();

            // 5. Create Lookup Table for Client Versions (NEW)
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + DatabaseConstants.CLIENT_VERSIONS_TABLE + "(" +
                            "id " + pkSyntax + ", " +
                            DatabaseConstants.CLIENT_VERSIONS_STRING_COLUMN + " VARCHAR(255) NOT NULL UNIQUE" +
                            ")"
            ).execute();
            connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_" + DatabaseConstants.CLIENT_VERSIONS_TABLE + "_string ON " + DatabaseConstants.CLIENT_VERSIONS_TABLE + "(" + DatabaseConstants.CLIENT_VERSIONS_STRING_COLUMN + ")"
            ).execute();

            // 6. Create Lookup Table for Server Versions (NEW)
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + DatabaseConstants.SERVER_VERSIONS_TABLE + "(" +
                            "id " + pkSyntax + ", " +
                            DatabaseConstants.SERVER_VERSIONS_STRING_COLUMN + " VARCHAR(255) NOT NULL UNIQUE" +
                            ")"
            ).execute();
            connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_" + DatabaseConstants.SERVER_VERSIONS_TABLE + "_string ON " + DatabaseConstants.SERVER_VERSIONS_TABLE + "(" + DatabaseConstants.SERVER_VERSIONS_STRING_COLUMN + ")"
            ).execute();


            // 7. Create Main Violations Table with ALL Foreign Keys and optimized UUID
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + DatabaseConstants.VIOLATIONS_TABLE + "(" +
                            "id " + pkSyntax + ", " +
                            DatabaseConstants.VIOLATIONS_SERVER_ID_COLUMN + " INTEGER NOT NULL, " +
                            DatabaseConstants.VIOLATIONS_UUID_COLUMN + " " + uuidType + " NOT NULL, " +
                            DatabaseConstants.VIOLATIONS_CHECK_NAME_ID_COLUMN + " INTEGER NOT NULL, " +
                            DatabaseConstants.VIOLATIONS_VERBOSE_COLUMN + " TEXT NOT NULL, " +
                            DatabaseConstants.VIOLATIONS_VL_COLUMN + " INTEGER NOT NULL, " +
                            DatabaseConstants.VIOLATIONS_CREATED_AT_COLUMN + " BIGINT NOT NULL, " +
                            DatabaseConstants.VIOLATIONS_GRIM_VERSION_ID_COLUMN + " INTEGER NOT NULL, " + // Grim's version
                            DatabaseConstants.VIOLATIONS_CLIENT_BRAND_ID_COLUMN + " INTEGER NOT NULL, " + // Client brand
                            DatabaseConstants.VIOLATIONS_CLIENT_VERSION_ID_COLUMN + " INTEGER NOT NULL, " + // Client version
                            DatabaseConstants.VIOLATIONS_SERVER_VERSION_ID_COLUMN + " INTEGER NOT NULL, " + // Server version
                            "FOREIGN KEY (" + DatabaseConstants.VIOLATIONS_SERVER_ID_COLUMN + ") REFERENCES " + DatabaseConstants.SERVERS_TABLE + "(id), " +
                            "FOREIGN KEY (" + DatabaseConstants.VIOLATIONS_CHECK_NAME_ID_COLUMN + ") REFERENCES " + DatabaseConstants.CHECK_NAMES_TABLE + "(id), " +
                            "FOREIGN KEY (" + DatabaseConstants.VIOLATIONS_GRIM_VERSION_ID_COLUMN + ") REFERENCES " + DatabaseConstants.GRIM_VERSIONS_TABLE + "(id), " +
                            "FOREIGN KEY (" + DatabaseConstants.VIOLATIONS_CLIENT_BRAND_ID_COLUMN + ") REFERENCES " + DatabaseConstants.CLIENT_BRANDS_TABLE + "(id), " +
                            "FOREIGN KEY (" + DatabaseConstants.VIOLATIONS_CLIENT_VERSION_ID_COLUMN + ") REFERENCES " + DatabaseConstants.CLIENT_VERSIONS_TABLE + "(id), " +
                            "FOREIGN KEY (" + DatabaseConstants.VIOLATIONS_SERVER_VERSION_ID_COLUMN + ") REFERENCES " + DatabaseConstants.SERVER_VERSIONS_TABLE + "(id)" +
                            ")"
            ).execute();

            // 8. Create Indexes for efficient querying on main table (includes all FKs)
            connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_" + DatabaseConstants.VIOLATIONS_TABLE + "_uuid ON " + DatabaseConstants.VIOLATIONS_TABLE + "(" + DatabaseConstants.VIOLATIONS_UUID_COLUMN + ")"
            ).execute();
            connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_" + DatabaseConstants.VIOLATIONS_TABLE + "_created_at ON " + DatabaseConstants.VIOLATIONS_TABLE + "(" + DatabaseConstants.VIOLATIONS_CREATED_AT_COLUMN + ")"
            ).execute();
            connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_" + DatabaseConstants.VIOLATIONS_TABLE + "_server_id ON " + DatabaseConstants.VIOLATIONS_TABLE + "(" + DatabaseConstants.VIOLATIONS_SERVER_ID_COLUMN + ")"
            ).execute();
            connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_" + DatabaseConstants.VIOLATIONS_TABLE + "_check_name_id ON " + DatabaseConstants.VIOLATIONS_TABLE + "(" + DatabaseConstants.VIOLATIONS_CHECK_NAME_ID_COLUMN + ")"
            ).execute();
            connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_" + DatabaseConstants.VIOLATIONS_TABLE + "_grim_version_id ON " + DatabaseConstants.VIOLATIONS_TABLE + "(" + DatabaseConstants.VIOLATIONS_GRIM_VERSION_ID_COLUMN + ")"
            ).execute();
            connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_" + DatabaseConstants.VIOLATIONS_TABLE + "_client_brand_id ON " + DatabaseConstants.VIOLATIONS_TABLE + "(" + DatabaseConstants.VIOLATIONS_CLIENT_BRAND_ID_COLUMN + ")"
            ).execute();
            connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_" + DatabaseConstants.VIOLATIONS_TABLE + "_client_version_id ON " + DatabaseConstants.VIOLATIONS_TABLE + "(" + DatabaseConstants.VIOLATIONS_CLIENT_VERSION_ID_COLUMN + ")"
            ).execute();
            connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_" + DatabaseConstants.VIOLATIONS_TABLE + "_server_version_id ON " + DatabaseConstants.VIOLATIONS_TABLE + "(" + DatabaseConstants.VIOLATIONS_SERVER_VERSION_ID_COLUMN + ")"
            ).execute();

        } catch (SQLException ex) {
            LogUtil.error("Failed to generate violations database:", ex);
            throw ex;
        }
    }

    @Override
    // Update signature to match the 4 new string parameters
    public synchronized void logAlert(GrimPlayer player, String grimVersion, String verbose, String checkName, int vls) {
        try (
                Connection connection = getConnection();
                PreparedStatement insertLog = connection.prepareStatement(
                        "INSERT INTO " + DatabaseConstants.VIOLATIONS_TABLE + " (" +
                                DatabaseConstants.VIOLATIONS_SERVER_ID_COLUMN + ", " +
                                DatabaseConstants.VIOLATIONS_UUID_COLUMN + ", " +
                                DatabaseConstants.VIOLATIONS_CHECK_NAME_ID_COLUMN + ", " +
                                DatabaseConstants.VIOLATIONS_VERBOSE_COLUMN + ", " +
                                DatabaseConstants.VIOLATIONS_VL_COLUMN + ", " +
                                DatabaseConstants.VIOLATIONS_CREATED_AT_COLUMN + ", " +
                                DatabaseConstants.VIOLATIONS_GRIM_VERSION_ID_COLUMN + ", " +
                                DatabaseConstants.VIOLATIONS_CLIENT_BRAND_ID_COLUMN + ", " +
                                DatabaseConstants.VIOLATIONS_CLIENT_VERSION_ID_COLUMN + ", " +
                                DatabaseConstants.VIOLATIONS_SERVER_VERSION_ID_COLUMN +
                                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" // Total 10 parameters
                )
        ) {
            String serverName = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("history.server-name", "Prison");
            long serverId = DatabaseUtils.getOrCreateId(connection, dialect, DatabaseConstants.SERVERS_TABLE, DatabaseConstants.SERVERS_STRING_COLUMN, serverName);
            long checkNameId = DatabaseUtils.getOrCreateId(connection, dialect, DatabaseConstants.CHECK_NAMES_TABLE, DatabaseConstants.CHECK_NAMES_STRING_COLUMN, checkName);
            long grimVersionId = DatabaseUtils.getOrCreateId(connection, dialect, DatabaseConstants.GRIM_VERSIONS_TABLE, DatabaseConstants.GRIM_VERSIONS_STRING_COLUMN, grimVersion);
            long clientBrandId = DatabaseUtils.getOrCreateId(connection, dialect, DatabaseConstants.CLIENT_BRANDS_TABLE, DatabaseConstants.CLIENT_BRANDS_STRING_COLUMN, player.getBrand());
            long clientVersionId = DatabaseUtils.getOrCreateId(connection, dialect, DatabaseConstants.CLIENT_VERSIONS_TABLE, DatabaseConstants.CLIENT_VERSIONS_STRING_COLUMN, player.getClientVersion().getReleaseName());
            long serverVersionId = DatabaseUtils.getOrCreateId(connection, dialect, DatabaseConstants.SERVER_VERSIONS_TABLE, DatabaseConstants.SERVER_VERSIONS_STRING_COLUMN, PacketEvents.getAPI().getServerManager().getVersion().toString());

            // Set parameters
            insertLog.setLong(1, serverId);
            insertLog.setBytes(2, DatabaseUtils.uuidToBytes(player.getUniqueId()));
            insertLog.setLong(3, checkNameId);
            insertLog.setString(4, verbose);
            insertLog.setInt(5, vls);
            insertLog.setLong(6, System.currentTimeMillis());
            insertLog.setLong(7, grimVersionId);
            insertLog.setLong(8, clientBrandId);
            insertLog.setLong(9, clientVersionId);
            insertLog.setLong(10, serverVersionId);

            insertLog.executeUpdate();
        } catch (SQLException ex) {
            LogUtil.error("Failed to insert violation:", ex);
        }
    }

    public synchronized int getLogCount(UUID player) {
        try (
                Connection connection = getConnection();
                PreparedStatement fetchLogs = connection.prepareStatement(
                        "SELECT COUNT(*) FROM " + DatabaseConstants.VIOLATIONS_TABLE + " WHERE " + DatabaseConstants.VIOLATIONS_UUID_COLUMN + " = ?"
                )
        ) {
            fetchLogs.setBytes(1, DatabaseUtils.uuidToBytes(player));
            ResultSet resultSet = fetchLogs.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (SQLException ex) {
            LogUtil.error("Failed to fetch number of violations:", ex);
        }
        return 0;
    }

    @Override
    public synchronized List<Violation> getViolations(UUID player, int page, int limit) {
        List<Violation> violations = new ArrayList<>();
        try (
                Connection connection = getConnection();
                PreparedStatement fetchLogs = connection.prepareStatement(
                        "SELECT " +
                                "v." + DatabaseConstants.VIOLATIONS_ID_COLUMN + ", " +
                                "s." + DatabaseConstants.SERVERS_STRING_COLUMN + ", " +
                                "v." + DatabaseConstants.VIOLATIONS_UUID_COLUMN + ", " +
                                "cn." + DatabaseConstants.CHECK_NAMES_STRING_COLUMN + ", " +
                                "v." + DatabaseConstants.VIOLATIONS_VERBOSE_COLUMN + ", " +
                                "v." + DatabaseConstants.VIOLATIONS_VL_COLUMN + ", " +
                                "v." + DatabaseConstants.VIOLATIONS_CREATED_AT_COLUMN + ", " +
                                "gv." + DatabaseConstants.GRIM_VERSIONS_STRING_COLUMN + ", " + // Select Grim's version string
                                "cb." + DatabaseConstants.CLIENT_BRANDS_STRING_COLUMN + ", " + // Select client brand string
                                "clv." + DatabaseConstants.CLIENT_VERSIONS_STRING_COLUMN + ", " + // Select client version string
                                "srv." + DatabaseConstants.SERVER_VERSIONS_STRING_COLUMN + " " + // Select server version string
                                "FROM " + DatabaseConstants.VIOLATIONS_TABLE + " v " +
                                "JOIN " + DatabaseConstants.SERVERS_TABLE + " s ON v." + DatabaseConstants.VIOLATIONS_SERVER_ID_COLUMN + " = s.id " +
                                "JOIN " + DatabaseConstants.CHECK_NAMES_TABLE + " cn ON v." + DatabaseConstants.VIOLATIONS_CHECK_NAME_ID_COLUMN + " = cn.id " +
                                "JOIN " + DatabaseConstants.GRIM_VERSIONS_TABLE + " gv ON v." + DatabaseConstants.VIOLATIONS_GRIM_VERSION_ID_COLUMN + " = gv.id " +
                                "JOIN " + DatabaseConstants.CLIENT_BRANDS_TABLE + " cb ON v." + DatabaseConstants.VIOLATIONS_CLIENT_BRAND_ID_COLUMN + " = cb.id " +
                                "JOIN " + DatabaseConstants.CLIENT_VERSIONS_TABLE + " clv ON v." + DatabaseConstants.VIOLATIONS_CLIENT_VERSION_ID_COLUMN + " = clv.id " +
                                "JOIN " + DatabaseConstants.SERVER_VERSIONS_TABLE + " srv ON v." + DatabaseConstants.VIOLATIONS_SERVER_VERSION_ID_COLUMN + " = srv.id " +
                                "WHERE v." + DatabaseConstants.VIOLATIONS_UUID_COLUMN + " = ? ORDER BY v." + DatabaseConstants.VIOLATIONS_CREATED_AT_COLUMN + " DESC LIMIT ? OFFSET ?"
                )
        ) {
            fetchLogs.setBytes(1, DatabaseUtils.uuidToBytes(player));
            fetchLogs.setInt(2, limit);
            fetchLogs.setInt(3, (page - 1) * limit);

            return Violation.fromResultSet(fetchLogs.executeQuery());
        } catch (SQLException ex) {
            LogUtil.error("Failed to fetch violations:", ex);
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
            LogUtil.error("Failed to close connection", ex);
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
