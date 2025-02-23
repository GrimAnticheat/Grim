package ac.grim.grimac.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;

public class GrimProfile implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager) {
        commandManager.command(
                commandManager.commandBuilder("grim", "grimac")
                        .literal("profile")
                        .permission("grim.profile")
                        .required("target", GrimAPI.INSTANCE.getParserDescriptors().getSinglePlayer())
//                        .suggester((context, input) -> {
//                            return Bukkit.getOnlinePlayers().stream()
//                                    .map(Player::getName)
//                                    .filter(name -> input.isEmpty() || name.toLowerCase().startsWith(input.toLowerCase()))
//                                    .collect(Collectors.toList());
//                        })
                        .handler(this::handleProfile)
        );
    }

    private void handleProfile(@NonNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        Player target = context.get("target");

        GrimPlayer grimPlayer = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(target.getUniqueId());

        if (grimPlayer.platformPlayer.isExternalPlayer()) {
            String alertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("player-not-this-server", "%prefix% &cThis player isn't on this server!");
            alertString = MessageUtil.replacePlaceholders(sender, alertString);
            sender.sendMessage(MessageUtil.miniMessage(alertString));
            return;
        }

        if (grimPlayer == null) {
            String message = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("player-not-found", "%prefix% &cPlayer is exempt or offline!");
            message = MessageUtil.replacePlaceholders(sender, message);
            sender.sendMessage(MessageUtil.miniMessage(message));
            return;
        }

        for (String message : GrimAPI.INSTANCE.getConfigManager().getConfig().getStringList("profile")) {
            final Component component = MessageUtil.miniMessage(message);
            sender.sendMessage(MessageUtil.replacePlaceholders(grimPlayer, component));
        }
    }
}
