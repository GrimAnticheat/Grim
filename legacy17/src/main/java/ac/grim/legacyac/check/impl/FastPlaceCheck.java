package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

public final class FastPlaceCheck extends Check {
    public FastPlaceCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "FastPlace");
    }

    public void onPlace(BlockPlaceEvent event, PlayerData data) {
        if (!isEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        if (isExempt(player, data)) {
            return;
        }

        int pps = data.incrementPlaceWindow();
        int maxPps = plugin.getConfig().getInt("checks.FastPlace.max-place-per-second", 14);
        if (pps > maxPps) {
            double buffer = increaseBuffer(data, 0.6D);
            if (buffer > plugin.getConfig().getDouble("checks.FastPlace.buffer", 1.5D)) {
                flag(player, data, 0.7D, "pps=" + pps);
            }
        }
    }
}
