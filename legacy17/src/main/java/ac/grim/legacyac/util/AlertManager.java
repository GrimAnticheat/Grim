package ac.grim.legacyac.util;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class AlertManager {
    private final LegacyAntiCheatPlugin plugin;
    private final Set<UUID> subscribers = ConcurrentHashMap.newKeySet();

    public AlertManager(LegacyAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean toggle(Player player) {
        UUID uuid = player.getUniqueId();
        if (subscribers.contains(uuid)) {
            subscribers.remove(uuid);
            return false;
        }
        subscribers.add(uuid);
        return true;
    }

    public void alert(Player suspect, String check, double vl, String detail) {
        String message = ChatColor.RED + "[GLAC] " + ChatColor.GRAY + suspect.getName() + " failed " + check
            + ChatColor.DARK_GRAY + " (VL=" + String.format("%.2f", vl) + ", " + detail + ")";
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (online.hasPermission("grimlegacy.alerts") && subscribers.contains(online.getUniqueId())) {
                online.sendMessage(message);
            }
        }
    }
}
