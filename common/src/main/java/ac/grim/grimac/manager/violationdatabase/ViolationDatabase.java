package ac.grim.grimac.manager.violationdatabase;

import ac.grim.grimac.player.GrimPlayer;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ViolationDatabase {

    void connect() throws SQLException;

    void logAlert(GrimPlayer player, String grimVersion, String verbose, String checkName, int vls);

    int getLogCount(UUID player);

    default CompletableFuture<Integer> getLogCountAsync(UUID player) {
        return CompletableFuture.completedFuture(getLogCount(player));
    }

    List<Violation> getViolations(UUID player, int page, int limit);

    default CompletableFuture<List<Violation>> getViolationsAsync(UUID player, int page, int limit) {
        return CompletableFuture.completedFuture(getViolations(player, page, limit));
    }

    void disconnect();

}
