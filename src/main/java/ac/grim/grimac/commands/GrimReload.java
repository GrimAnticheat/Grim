package ac.grim.grimac.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import org.bukkit.command.CommandSender;

@CommandAlias("grim|grimac")
public class GrimReload extends BaseCommand {
    @Subcommand("reload")
    @CommandPermission("grim.reload")
    public void onReload(CommandSender sender) {
        // reload config
        String reloading = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("reloading", "%prefix% &7Reloading config...");
        MessageUtil.sendMessage(sender, MessageUtil.miniMessage(MessageUtil.replacePlaceholders(sender, reloading)));
        GrimAPI.INSTANCE.getExternalAPI().reloadAsync().exceptionally(throwable -> false)
                .thenAccept(bool -> {
                    String message = bool
                            ? GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("reloaded", "%prefix% &fConfig has been reloaded.")
                            : GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("reload-failed", "%prefix% &cFailed to reload config.");
                    MessageUtil.sendMessage(sender, MessageUtil.miniMessage(MessageUtil.replacePlaceholders(sender, message)));
                });
    }
}
