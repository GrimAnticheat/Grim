package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class AutoClickerCheck extends Check {
    public AutoClickerCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "AutoClicker");
    }

    public void onInteract(PlayerInteractEvent event, PlayerData data) {
        if (!isEnabled()) {
            return;
        }
        if (isExempt(event.getPlayer(), data)) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        long now = System.currentTimeMillis();
        if (data.getClickWindowStart() == 0L || now - data.getClickWindowStart() > 1000L) {
            data.setClickWindowStart(now);
            data.setClickWindow(0);
        }

        data.setClickWindow(data.getClickWindow() + 1);
        int cps = data.getClickWindow();
        int maxCps = plugin.getConfig().getInt("checks.AutoClicker.max-cps", 17);
        if (cps > maxCps) {
            double buffer = increaseBuffer(data, 0.6D);
            if (buffer > plugin.getConfig().getDouble("checks.AutoClicker.buffer", 1.5D)) {
                flag(event.getPlayer(), data, 0.6D, "cps=" + cps);
            }
        }
    }
}
