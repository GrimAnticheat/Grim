package ac.grim.grimac.manager.violationdatabase;

import ac.grim.grimac.player.GrimPlayer;

import java.util.List;
import java.util.UUID;

public final class NoOpViolationDatabase implements ViolationDatabase {
    public static final NoOpViolationDatabase INSTANCE = new NoOpViolationDatabase();
    private NoOpViolationDatabase() {}

    @Override public void connect()    { }
    @Override public void disconnect() {}
    @Override public void logAlert(GrimPlayer p, String grimVersion, String v, String c, int vl) {}
    @Override public int getLogCount(UUID player) { return 0; }
    @Override public List<Violation> getViolations(UUID p, int page, int lim) { return List.of(); }
}
