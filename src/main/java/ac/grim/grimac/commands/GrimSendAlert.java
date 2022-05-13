package ac.grim.grimac.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.events.packets.PacketPluginMessage;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import org.bukkit.entity.Player;

@CommandAlias("grim|grimac")
public class GrimSendAlert extends BaseCommand {
    @Subcommand("sendalert")
    @CommandPermission("grim.sendalert")
    public void sendAlert(String string) {
        string = MessageUtil.format(string);

        for (Player bukkitPlayer : GrimAPI.INSTANCE.getAlertManager().getEnabledAlerts()) {
            bukkitPlayer.sendMessage(string);
        }
        
        if (GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("alerts.bungeecord.send", false)) {
        	PacketPluginMessage.sendPluginMessage(string);
        }

        if (GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("alerts.print-to-console", true)) {
            LogUtil.info(string); // Print alert to console
        }
    }
}
