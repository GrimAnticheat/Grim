package ac.grim.grimac.manager.violationdatabase;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record Violation(String server, UUID uuid, String checkName, String verbose, int vl,
                                long createdAt, String version) {

    public static List<Violation> fromResultSet(ResultSet resultSet) throws SQLException {
        List<Violation> violations = new ArrayList<>();
        while (resultSet.next()) {
            String server = resultSet.getString("server_name");
            byte[] uuidBytes = resultSet.getBytes("uuid");
            UUID uuid = DatabaseUtils.bytesToUuid(uuidBytes);
            String checkName = resultSet.getString("check_name_string");
            String verbose = resultSet.getString("verbose");
            int vl = resultSet.getInt("vl");
            long createdAt = resultSet.getLong("created_at");
            String version = resultSet.getString("version_string");

            violations.add(new Violation(server, uuid, checkName, verbose, vl, createdAt, version));
        }
        return violations;
    }
}
