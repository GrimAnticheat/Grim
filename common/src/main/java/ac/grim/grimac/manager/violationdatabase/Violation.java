package ac.grim.grimac.manager.violationdatabase;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record Violation(String server, UUID uuid, String checkName, String verbose, int vl,
                        long createdAt, String grimVersion, String clientBrand, String clientVersion, String serverVersion) {

    public static List<Violation> fromResultSet(ResultSet resultSet) throws SQLException {
        List<Violation> violations = new ArrayList<>();
        while (resultSet.next()) {
            String server = resultSet.getString(DatabaseConstants.SERVERS_STRING_COLUMN);
            byte[] uuidBytes = resultSet.getBytes(DatabaseConstants.VIOLATIONS_UUID_COLUMN);
            UUID uuid = DatabaseUtils.bytesToUuid(uuidBytes);
            String checkName = resultSet.getString(DatabaseConstants.CHECK_NAMES_STRING_COLUMN);
            String verbose = resultSet.getString(DatabaseConstants.VIOLATIONS_VERBOSE_COLUMN);
            int vl = resultSet.getInt(DatabaseConstants.VIOLATIONS_VL_COLUMN);
            long createdAt = resultSet.getLong(DatabaseConstants.VIOLATIONS_CREATED_AT_COLUMN);
            String grimVersion = resultSet.getString(DatabaseConstants.GRIM_VERSIONS_STRING_COLUMN);
            String clientBrand = resultSet.getString(DatabaseConstants.CLIENT_BRANDS_STRING_COLUMN);
            String clientVersion = resultSet.getString(DatabaseConstants.CLIENT_VERSIONS_STRING_COLUMN);
            String serverVersion = resultSet.getString(DatabaseConstants.SERVER_VERSIONS_STRING_COLUMN);

            violations.add(new Violation(server, uuid, checkName, verbose, vl, createdAt, grimVersion, clientBrand, clientVersion, serverVersion));
        }
        return violations;
    }
}
