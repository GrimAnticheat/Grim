package ac.grim.grimac.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.GrimUser;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import net.kyori.adventure.text.Component;

@CommandAlias("grim|grimac")
public class GrimSendAlert extends BaseCommand {
    @Subcommand("sendalert")
    @CommandPermission("grim.sendalert")
    public void sendAlert(String string) {
        string = MessageUtil.replacePlaceholders((Object) null, string);
        Component message = MessageUtil.miniMessage(string);

        for (GrimUser grimUser : GrimAPI.INSTANCE.getAlertManager().getEnabledAlerts()) {
            MessageUtil.sendMessage((GrimPlayer) grimUser, message);
        }

        if (GrimAPI.INSTANCE.getConfigManager().isPrintAlertsToConsole()) {
            LogUtil.console(message); // Print alert to console
        }
    }
}
