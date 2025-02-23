package ac.grim.grimac.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;

public class GrimReload implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager) {
        commandManager.command(
                commandManager.commandBuilder("grim", "grimac")
                        .literal("reload")
                        .permission("grim.reload")
                        .handler(this::handleReload)
        );
    }

    private void handleReload(@NonNull CommandContext<Sender> context) {
        Sender sender = context.sender();

        // reload config
        String reloading = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("reloading", "%prefix% &7Reloading config...");
        sender.sendMessage(MessageUtil.miniMessage(MessageUtil.replacePlaceholders(sender, reloading)));

        GrimAPI.INSTANCE.getExternalAPI().reloadAsync().exceptionally(throwable -> false)
                .thenAccept(bool -> {
                    String message = bool
                            ? GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("reloaded", "%prefix% &fConfig has been reloaded.")
                            : GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("reload-failed", "%prefix% &cFailed to reload config.");
                    sender.sendMessage(MessageUtil.miniMessage(MessageUtil.replacePlaceholders(sender, message)));
                });
    }
}
