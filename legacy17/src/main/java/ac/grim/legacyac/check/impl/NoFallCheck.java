package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

public final class NoFallCheck extends Check {
    public NoFallCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "NoFall");
    }

    public void onFallDamage(EntityDamageEvent event, Player player, PlayerData data) {
        if (!isEnabled()) {
            return;
        }
        if (isExempt(player, data)) {
            return;
        }

        if (event.getDamage() <= 0.0D) {
            return;
        }

        if (data.getAirTicks() <= plugin.getConfig().getInt("checks.NoFall.min-air-ticks", 6)) {
            double buffer = increaseBuffer(data, 1.0D);
            if (buffer > plugin.getConfig().getDouble("checks.NoFall.buffer", 1.5D)) {
                flag(player, data, 1.0D, "airTicks=" + data.getAirTicks() + " damage=" + String.format("%.2f", event.getDamage()));
            }
        }
    }
}
