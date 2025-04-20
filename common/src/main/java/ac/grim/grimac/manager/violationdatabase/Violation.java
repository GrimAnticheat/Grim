package ac.grim.grimac.manager.violationdatabase;

import lombok.Data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
public class Violation {

    private final String serverName;
    private final UUID playerUUID;
    private final String checkName;
    private final String verbose;
    private final int vl;
    private final Date createdAt;

    public static List<Violation> fromResultSet(ResultSet resultSet) throws SQLException {
        List<Violation> violations = new ArrayList<>();
        while(resultSet.next()) {
            String server = resultSet.getString("server");
            UUID player = UUID.fromString(resultSet.getString("uuid"));
            String checkName = resultSet.getString("check_name");
            String verbose = resultSet.getString("verbose");
            int vl = resultSet.getInt("vl");
            Date createdAt = new Date(resultSet.getLong("created_at"));

            violations.add(new Violation(server, player, checkName, verbose, vl, createdAt));
        }

        return violations;
    }

}
