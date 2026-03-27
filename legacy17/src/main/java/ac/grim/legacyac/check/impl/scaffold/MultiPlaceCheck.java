package ac.grim.legacyac.check.impl.scaffold;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * MultiPlace — Multiple blocks placed in a single game tick.
 * Ported from Grim's MultiPlace check.
 *
 * Vanilla Minecraft only allows one block placement per tick.
 * Scaffold/tower hacks often place multiple blocks per tick for speed.
 */
public final class MultiPlaceCheck extends Check {
    public MultiPlaceCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "MultiPlace");
    }

    public void onPlace(BlockPlaceEvent event, PlayerData data) {
        if (!isEnabled() || isExempt(event.getPlayer(), data)) {
            return;
        }

        long now = System.currentTimeMillis();
        long lastPlace = data.getLastBlockPlaceTimeMs();
        int serverTickMs = 50; // 1 tick = 50ms

        if (lastPlace != 0 && (now - lastPlace) < serverTickMs) {
            int sameTickCount = data.incrementSameTickPlaceCount();
            int maxPerTick = plugin.getConfig().getInt("checks.MultiPlace.max-per-tick", 1);

            if (sameTickCount > maxPerTick) {
                double buffer = increaseBuffer(data, 1.0);
                if (buffer > plugin.getConfig().getDouble("checks.MultiPlace.buffer", 2.0)) {
                    flag(event.getPlayer(), data, 1.0,
                            "placesThisTick=" + sameTickCount
                                    + " interval=" + (now - lastPlace) + "ms");
                }
            }
        } else {
            data.resetSameTickPlaceCount();
        }

        data.setLastBlockPlaceTimeMs(now);
    }

    public void onPacketPlace(Player player, PlayerData data, PlayerData.QueuedBlockPlaceSnapshot snapshot) {
        if (!isEnabled() || isExempt(player, data)) {
            return;
        }

        long now = System.currentTimeMillis();
        long lastPlace = data.getLastBlockPlaceTimeMs();
        int serverTickMs = 50;
        if (lastPlace != 0L && (now - lastPlace) < serverTickMs) {
            int sameTickCount = data.incrementSameTickPlaceCount();
            int maxPerTick = plugin.getConfig().getInt("checks.MultiPlace.max-per-tick", 1);
            if (sameTickCount > maxPerTick) {
                double buffer = increaseBuffer(data, 1.0D);
                if (buffer > plugin.getConfig().getDouble("checks.MultiPlace.buffer", 2.0D)) {
                    flag(player, data, 1.0D,
                            "placesThisTick=" + sameTickCount
                                    + " interval=" + (now - lastPlace) + "ms");
                }
            }
        } else {
            data.resetSameTickPlaceCount();
        }
        data.setLastBlockPlaceTimeMs(now);
    }
}
