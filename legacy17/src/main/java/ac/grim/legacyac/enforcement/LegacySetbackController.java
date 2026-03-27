package ac.grim.legacyac.enforcement;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.data.PlayerData;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Central correction controller for setback/resync requests.
 */
public final class LegacySetbackController {
    private final LegacyAntiCheatPlugin plugin;
    private final Map<UUID, Long> lastCorrectionAt = new ConcurrentHashMap<UUID, Long>();

    public LegacySetbackController(LegacyAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean requestCorrection(Player player, PlayerData data, CorrectionReason reason,
            CorrectionSeverity severity, String detail) {
        if (player == null || data == null || !player.isOnline()) {
            return false;
        }

        long now = System.currentTimeMillis();
        Long previous = lastCorrectionAt.get(player.getUniqueId());
        long lastAt = previous == null ? 0L : previous.longValue();

        if (severity == CorrectionSeverity.HARD) {
            if (data.getLastSafeLocation() == null || data.isTeleportSyncPending() || now - lastAt < 500L) {
                return false;
            }
            Location target = data.getLastSafeLocation().clone();
            lastCorrectionAt.put(player.getUniqueId(), Long.valueOf(now));
            if (data.isDebugEnabled()) {
                plugin.getLogger().info("[GLAC-DEBUG] " + player.getName() + " correction HARD " + reason.name()
                        + (detail == null ? "" : " " + detail));
            }
            player.teleport(target);
            return true;
        }

        if (data.isTeleportSyncPending() || now - lastAt < 150L) {
            return false;
        }

        Location target = data.getLastSafeLocation() != null ? data.getLastSafeLocation().clone() : player.getLocation();
        lastCorrectionAt.put(player.getUniqueId(), Long.valueOf(now));
        if (data.isDebugEnabled()) {
            plugin.getLogger().info("[GLAC-DEBUG] " + player.getName() + " correction SOFT " + reason.name()
                    + (detail == null ? "" : " " + detail));
        }
        player.teleport(target);
        return true;
    }

    public enum CorrectionReason {
        CHECK_VIOLATION,
        VELOCITY,
        REACH,
        RESYNC
    }

    public enum CorrectionSeverity {
        SOFT,
        HARD
    }
}
