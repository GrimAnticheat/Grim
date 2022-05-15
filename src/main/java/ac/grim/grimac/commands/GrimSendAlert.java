package ac.grim.grimac.commands;

import org.bukkit.entity.Player;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.events.packets.AlertPluginMessenger;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;

@CommandAlias("grim|grimac")
public class GrimSendAlert extends BaseCommand {
    @Subcommand("sendalert")
    @CommandPermission("grim.sendalert")
    public void sendAlert(String string) {
        string = MessageUtil.format(string);

        for (Player bukkitPlayer : GrimAPI.INSTANCE.getAlertManager().getEnabledAlerts()) {
            bukkitPlayer.sendMessage(string);
        }
        
        if (AlertPluginMessenger.canSendAlerts()) {
        	AlertPluginMessenger.sendPluginMessage(string);
        }

        if (GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("alerts.print-to-console", true)) {
            LogUtil.info(string); // Print alert to console
        }
    }
}
