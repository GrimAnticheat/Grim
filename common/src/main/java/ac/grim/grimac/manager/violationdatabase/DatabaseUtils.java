package ac.grim.grimac.manager.violationdatabase;

import ac.grim.grimac.utils.anticheat.LogUtil;
import lombok.experimental.UtilityClass;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@UtilityClass
public class DatabaseUtils {

    public static byte[] uuidToBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    public static UUID bytesToUuid(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            throw new IllegalArgumentException("UUID bytes must be 16 bytes long. Received: " + (bytes == null ? "null" : bytes.length + " bytes"));
        }
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long msb = bb.getLong();
        long lsb = bb.getLong();
        return new UUID(msb, lsb);
    }


    /**
     * Retrieves a UUID from a ResultSet.
     * This handles both PostgreSQL (which returns a native {@link UUID} object)
     * and MySQL/SQLite (which return a {@code byte[]}).
     *
     * @param resultSet  The ResultSet to read from.
     * @param columnName The name of the UUID column.
     * @return The UUID.
     * @throws SQLException If a database access error occurs, or if the UUID column
     *                      contains an unexpected type or invalid data.
     */
    public static UUID getUuid(ResultSet resultSet, String columnName) throws SQLException {
        Object uuidObject = resultSet.getObject(columnName);

        if (uuidObject instanceof byte[] uuidBytes) {
            return bytesToUuid(uuidBytes);
        } else if (uuidObject instanceof UUID uuid) {
            return uuid;
        }

        throw new SQLException("Unexpected UUID type: " + (uuidObject == null ? "null" : uuidObject.getClass().getName()));
    }

    // --- Generic Deduplication Lookup (uses DatabaseDialect) ---
    public static long getOrCreateId(Connection connection, DatabaseDialect dialect, String tableName, String stringColumnName, String value) throws SQLException {
        // Step 1: Attempt to insert the string.
        String insertSql = dialect.getInsertOrIgnoreSyntax(tableName, stringColumnName);

        try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
            insertStmt.setString(1, value);
            insertStmt.executeUpdate();
        } catch (SQLException e) {
            // Check for specific unique constraint violation error codes/messages using the dialect
            if (!(e.getSQLState().equals(dialect.getUniqueConstraintViolationSQLState()) &&
                    e.getErrorCode() == dialect.getUniqueConstraintViolationErrorCode())) {
                LogUtil.error("Failed to insert into " + tableName + ": " + value, e);
                throw e; // Re-throw if it's a critical error not related to unique constraint
            }
        }

        // Step 2: Retrieve the ID (either newly generated or existing)
        try (PreparedStatement selectStmt = connection.prepareStatement(
                "SELECT id FROM " + tableName + " WHERE " + stringColumnName + " = ?"
        )) {
            selectStmt.setString(1, value);
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                } else {
                    throw new SQLException("Failed to retrieve ID for " + value + " from " + tableName);
                }
            }
        }
    }
}
