package ac.grim.grimac.manager.violationdatabase;

import java.util.UUID;

public record HistoryPlayer(UUID uuid, String username, long firstSeen, long lastSeen) {
}
