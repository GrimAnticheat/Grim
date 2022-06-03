package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import com.github.puregero.multilib.MultiLib;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class AlertManager {
    public AlertManager() {
        MultiLib.onString(GrimAPI.INSTANCE.getPlugin(), "grimac:alerts", string -> {
            String[] args = string.split("\t");
            if (args.length == 2) {
                String playerUUID = args[0];
                String state = args[1];
                Bukkit.getScheduler().runTaskAsynchronously(GrimAPI.INSTANCE.getPlugin(), () -> {
                    Player player = Bukkit.getPlayer(playerUUID);
                    if (player != null) {
                        if (state.equals("true")) {
                            enabledAlerts.add(player);
                        } else {
                            enabledAlerts.remove(player);
                        }
                    }
                });
            }
        });
    }

    @Getter
    private final Set<Player> enabledAlerts = new CopyOnWriteArraySet<>(new HashSet<>());

    public void toggle(Player player) {
        if (!enabledAlerts.remove(player)) {
            String alertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("alerts-enabled", "%prefix% &fAlerts enabled");
            alertString = MessageUtil.format(alertString);
            player.sendMessage(alertString);

            enabledAlerts.add(player);
        } else {
            String alertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("alerts-disabled", "%prefix% &fAlerts disabled");
            alertString = MessageUtil.format(alertString);
            player.sendMessage(alertString);
        }
        handleMultiPaper(player, enabledAlerts.contains(player));
    }

    private void handleMultiPaper(Player player, boolean enabled) {
        if (enabled) {
            MultiLib.notify("grimac:alerts", player.getUniqueId() + "\t" + "true");
        } else {
            MultiLib.notify("grimac:alerts", player.getUniqueId() + "\t" + "false");
        }
    }

    public void handlePlayerQuit(Player player) {
        enabledAlerts.remove(player);
        handleMultiPaper(player, false);
    }
}
