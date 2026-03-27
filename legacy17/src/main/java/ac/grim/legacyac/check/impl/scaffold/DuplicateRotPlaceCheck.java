package ac.grim.legacyac.check.impl.scaffold;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

public final class DuplicateRotPlaceCheck extends Check {
    private final Map<UUID, Float> lastPlacedYawDeltas = new ConcurrentHashMap<UUID, Float>();

    public DuplicateRotPlaceCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "DuplicateRotPlace");
    }

    public void onPlace(BlockPlaceEvent event, PlayerData data) {
        if (!isEnabled() || isExempt(event.getPlayer(), data)) {
            return;
        }
        Player player = event.getPlayer();
        float yawDelta = Math.abs(data.getLastYawDelta());
        if (yawDelta <= 2.0F) {
            return;
        }
        Float lastPlacedDelta = lastPlacedYawDeltas.get(player.getUniqueId());
        double diff = lastPlacedDelta == null ? Double.MAX_VALUE : Math.abs(yawDelta - lastPlacedDelta.floatValue());
        if (lastPlacedDelta != null && diff < 1.0E-4D) {
            double buffer = increaseBuffer(data, 1.0D);
            if (buffer > plugin.getConfig().getDouble("checks.DuplicateRotPlace.buffer", 1.25D)) {
                flag(player, data, 1.0D, "yawDelta=" + String.format(java.util.Locale.ROOT, "%.4f", yawDelta));
            }
        } else {
            coolDownScore(data);
        }
        lastPlacedYawDeltas.put(player.getUniqueId(), Float.valueOf(yawDelta));
    }

    public void onPacketPlace(Player player, PlayerData data) {
        if (!isEnabled() || isExempt(player, data)) {
            return;
        }
        float yawDelta = Math.abs(data.getLastYawDelta());
        if (yawDelta <= 2.0F) {
            return;
        }
        Float lastPlacedDelta = lastPlacedYawDeltas.get(player.getUniqueId());
        double diff = lastPlacedDelta == null ? Double.MAX_VALUE : Math.abs(yawDelta - lastPlacedDelta.floatValue());
        if (lastPlacedDelta != null && diff < 1.0E-4D) {
            double buffer = increaseBuffer(data, 1.0D);
            if (buffer > plugin.getConfig().getDouble("checks.DuplicateRotPlace.buffer", 1.25D)) {
                flag(player, data, 1.0D,
                        "yawDelta=" + String.format(java.util.Locale.ROOT, "%.4f", yawDelta));
            }
        } else {
            coolDownScore(data);
        }
        lastPlacedYawDeltas.put(player.getUniqueId(), Float.valueOf(yawDelta));
    }
}
