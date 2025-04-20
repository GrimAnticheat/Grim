package ac.grim.grimac.manager.violationdatabase;

import ac.grim.grimac.player.GrimPlayer;

import java.util.List;
import java.util.UUID;

public interface ViolationDatabase {

    void connect();

    void logAlert(GrimPlayer player, String verbose, String checkName, int vls);

    int getLogCount(UUID player);

    List<Violation> getViolations(UUID player, int page, int limit);

    void disconnect();

}
