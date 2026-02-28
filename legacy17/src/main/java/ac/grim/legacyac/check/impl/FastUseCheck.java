package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public final class FastUseCheck extends Check {
    public FastUseCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "FastUse");
    }

    public void onConsume(PlayerItemConsumeEvent event, PlayerData data) {
        if (!isEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        if (isExempt(player, data)) {
            return;
        }

        int uses = data.incrementUseWindow();
        int maxUses = plugin.getConfig().getInt("checks.FastUse.max-consume-per-second", 2);
        if (uses > maxUses) {
            double buffer = increaseBuffer(data, 1.0D);
            if (buffer > plugin.getConfig().getDouble("checks.FastUse.buffer", 1.0D)) {
                flag(player, data, 1.0D, "uses=" + uses);
            }
        }
    }
}
