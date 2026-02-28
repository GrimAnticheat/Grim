package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

public final class FastBreakCheck extends Check {
    public FastBreakCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "FastBreak");
    }

    public void onBreak(BlockBreakEvent event, PlayerData data) {
        if (!isEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        if (isExempt(player, data)) {
            return;
        }

        Material type = event.getBlock().getType();
        if (type == Material.LEAVES || type == Material.LOG || type == Material.STONE) {
            int bps = data.incrementBreakWindow();
            int maxBps = plugin.getConfig().getInt("checks.FastBreak.max-break-per-second", 12);
            if (bps > maxBps) {
                double buffer = increaseBuffer(data, 0.6D);
                if (buffer > plugin.getConfig().getDouble("checks.FastBreak.buffer", 1.5D)) {
                    flag(player, data, 0.8D, "bps=" + bps + " block=" + type.name());
                }
            }
        }
    }
}
