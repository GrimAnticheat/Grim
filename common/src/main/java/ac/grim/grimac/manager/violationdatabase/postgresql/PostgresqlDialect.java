package ac.grim.grimac.manager.violationdatabase.postgresql;

import ac.grim.grimac.manager.violationdatabase.DatabaseDialect;

public class PostgresqlDialect implements DatabaseDialect {

    @Override
    public String getUuidColumnType() {
        return "UUID";
    }

    @Override
    public String getAutoIncrementPrimaryKeySyntax() {
        return "BIGSERIAL PRIMARY KEY";
    }

    @Override
    public String getInsertOrIgnoreSyntax(String tableName, String columnNames) {
        return "INSERT INTO " + tableName + " (" + columnNames + ") VALUES (?) ON CONFLICT DO NOTHING";
    }

    @Override
    public String getUniqueConstraintViolationSQLState() {
        return "23505"; // Postgresql duplicate key error
    }

    @Override
    public int getUniqueConstraintViolationErrorCode() {
        return 0; // Postgresql is not using numbers
    }
}
