package ac.grim.grimac.manager.violationdatabase.mysql;

import ac.grim.grimac.manager.violationdatabase.DatabaseDialect;

public class MySQLDialect implements DatabaseDialect {

    @Override
    public String getUuidColumnType() {
        return "BINARY(16)";
    }

    @Override
    public String getAutoIncrementPrimaryKeySyntax() {
        return "BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT";
    }

    @Override
    public String getInsertOrIgnoreSyntax(String tableName, String columnNames) {
        return "INSERT IGNORE INTO " + tableName + " (" + columnNames + ") VALUES (?)";
    }

    @Override
    public String getUniqueConstraintViolationSQLState() {
        return "23000"; // Generic SQLSTATE for integrity constraint violation
    }

    @Override
    public int getUniqueConstraintViolationErrorCode() {
        return 1062; // MySQL specific error code for Duplicate entry
    }
}
