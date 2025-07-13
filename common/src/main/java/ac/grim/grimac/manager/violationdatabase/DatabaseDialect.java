package ac.grim.grimac.manager.violationdatabase;

public interface DatabaseDialect {

    /**
     * Returns the appropriate SQL column type for storing a 16-byte UUID.
     * E.g., "BLOB" for SQLite, "BINARY(16)" for MySQL.
     */
    String getUuidColumnType();

    /**
     * Returns the SQL syntax for creating an auto-incrementing primary key.
     * E.g., "INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT" for SQLite,
     * "BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT" for MySQL.
     */
    String getAutoIncrementPrimaryKeySyntax();

    /**
     * Returns the SQL syntax for an INSERT statement that ignores duplicate unique keys.
     * E.g., "INSERT OR IGNORE INTO tableName (columnName) VALUES (?)" for SQLite,
     * "INSERT IGNORE INTO tableName (columnName) VALUES (?)" for MySQL.
     *
     * @param tableName The name of the table to insert into.
     * @param columnNames The column names to insert into (comma-separated).
     */
    String getInsertOrIgnoreSyntax(String tableName, String columnNames);

    /**
     * Returns the SQL state or error code for a unique constraint violation.
     * Used to differentiate expected "duplicate key" errors from other SQLExceptions.
     */
    String getUniqueConstraintViolationSQLState();
    int getUniqueConstraintViolationErrorCode();
}
