package ac.grim.grimac.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@CommandAlias("grim|grimac")
public class GrimSendAlert extends BaseCommand {
    @Subcommand("sendalert")
    @CommandPermission("grim.sendalert")
    public void sendAlert(String string) {
        Component message = MessageUtil.miniMessage(string);

        for (Player bukkitPlayer : GrimAPI.INSTANCE.getAlertManager().getEnabledAlerts()) {
            MessageUtil.sendMessage(bukkitPlayer, message);
        }

        if (GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("alerts.print-to-console", true)) {
            LogUtil.console(message); // Print alert to console
        }
    }
}
