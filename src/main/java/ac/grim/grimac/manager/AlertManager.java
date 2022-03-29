package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class AlertManager {
    private final Set<Player> enabledAlerts = new CopyOnWriteArraySet<>(new HashSet<>());

    public void toggle(Player player) {
        if (!enabledAlerts.remove(player)) {
            String alertString = GrimAPI.INSTANCE.getPlugin().getConfig().getString("alerts-enabled", "%prefix% &fAlerts enabled");
            alertString = MessageUtil.format(alertString);
            player.sendMessage(alertString);

            enabledAlerts.add(player);
        } else {
            String alertString = GrimAPI.INSTANCE.getPlugin().getConfig().getString("alerts-disabled", "%prefix% &fAlerts disabled");
            alertString = MessageUtil.format(alertString);
            player.sendMessage(alertString);
        }
    }

    public void handlePlayerQuit(Player player) {
        enabledAlerts.remove(player);
    }

    public void sendAlert(GrimPlayer player, String verbose, String checkName, String violations) {
        String alertString = GrimAPI.INSTANCE.getPlugin().getConfig().getString("alerts.format", "%prefix% &f%player% &bfailed &f%check_name% &f(x&c%vl%&f) &7%verbose%");
        if (player.bukkitPlayer != null) {
            alertString = alertString.replace("%player%", player.bukkitPlayer.getName());
        }
        alertString = alertString.replace("%check_name%", checkName);
        alertString = alertString.replace("%vl%", violations);
        alertString = alertString.replace("%verbose%", verbose);
        alertString = MessageUtil.format(alertString);

        if (!GrimAPI.INSTANCE.getPlugin().getConfig().getBoolean("test-mode", false)) {
            for (Player bukkitPlayer : enabledAlerts) {
                bukkitPlayer.sendMessage(alertString);
            }
        } else {
            player.bukkitPlayer.sendMessage(alertString);
        }

        GrimAPI.INSTANCE.getDiscordManager().sendAlert(player, verbose, checkName, violations);
    }
}
