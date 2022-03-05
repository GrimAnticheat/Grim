package ac.grim.grimac.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@CommandAlias("grim|grimac")
public class GrimAlerts extends BaseCommand {
    private static final List<Player> disabledAlerts = new CopyOnWriteArrayList<>(new ArrayList<>());

    public static void toggle(Player player) {
        if (disabledAlerts.contains(player)) {
            String alertString = GrimAPI.INSTANCE.getPlugin().getConfig().getString("messages.alerts-enabled", "%prefix% &fAlerts enabled");
            alertString = MessageUtil.format(alertString);
            player.sendMessage(alertString);

            disabledAlerts.remove(player);
        } else {
            String alertString = GrimAPI.INSTANCE.getPlugin().getConfig().getString("messages.alerts-disabled", "%prefix% &fAlerts disabled");
            alertString = MessageUtil.format(alertString);
            player.sendMessage(alertString);

            disabledAlerts.add(player);
        }
    }

    public static boolean isAlertDisabled(Player player) {
        return disabledAlerts.contains(player);
    }

    public static void handlePlayerQuit(Player player) {
        disabledAlerts.remove(player);
    }

    @Subcommand("alerts")
    @CommandPermission("grim.alerts")
    public void onAlerts(Player player) {
        toggle(player);
    }
}
