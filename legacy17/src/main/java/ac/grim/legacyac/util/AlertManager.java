package ac.grim.legacyac.util;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import java.util.Locale;

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

    public boolean isSubscribed(Player player) {
        return subscribers.contains(player.getUniqueId());
    }

    public void alert(Player suspect, String check, double vl, String detail) {
        String message = ChatColor.RED + "[GLAC] " + ChatColor.GRAY + suspect.getName() + " failed " + check
            + ChatColor.DARK_GRAY + " (VL=" + String.format(Locale.ROOT, "%.2f", vl) + ", " + detail + ")";

        if (plugin.getConfig().getBoolean("alerts.console", true)) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.stripColor(message));
        }

        boolean requireSubscription = plugin.getConfig().getBoolean("alerts.require-subscription", false);
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (!online.hasPermission("grimlegacy.alerts")) {
                continue;
            }
            if (requireSubscription && !subscribers.contains(online.getUniqueId())) {
                continue;
            }
            online.sendMessage(message);
        }
    }
}
