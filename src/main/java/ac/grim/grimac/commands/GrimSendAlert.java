package ac.grim.grimac.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.GrimUser;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;

public class GrimSendAlert implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager) {
        commandManager.command(
                commandManager.commandBuilder("grim", "grimac")
                        .literal("sendalert")
                        .permission("grim.sendalert")
                        .required("message", StringParser.greedyStringParser())
                        .handler(this::handleSendAlert)
        );
    }

    private void handleSendAlert(@NonNull CommandContext<Sender> context) {
        String string = context.get("message");

        string = MessageUtil.replacePlaceholders((Object) null, string);
        Component message = MessageUtil.miniMessage(string);

        for (GrimUser grimUser : GrimAPI.INSTANCE.getAlertManager().getEnabledAlerts()) {
            ((GrimPlayer) grimUser).sendMessage(message);
        }

        if (GrimAPI.INSTANCE.getConfigManager().isPrintAlertsToConsole()) {
            LogUtil.console(message); // Print alert to console
        }
    }
}
