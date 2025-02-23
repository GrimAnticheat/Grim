package ac.grim.grimac.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.api.sender.Sender;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;

public class GrimSpectate implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager) {
        commandManager.command(
                commandManager.commandBuilder("grim", "grimac")
                        .literal("spectate")
                        .permission("grim.spectate")
                        .optional("target", GrimAPI.INSTANCE.getParserDescriptors().getSinglePlayer())
//                        .suggester((context, input) -> {
//                            return Bukkit.getOnlinePlayers().stream()
//                                    .map(Player::getName)
//                                    .filter(name -> input.isEmpty() || name.toLowerCase().startsWith(input.toLowerCase()))
//                                    .collect(Collectors.toList());
//                        })
                        .handler(this::handleSpectate)
        );
    }

    private void handleSpectate(@NonNull CommandContext<Sender> context) {
        Sender sender = context.sender();

//        if (!(sender.bukkit() instanceof Player p)) return;
        if (sender.isConsole()) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return;
        }

        Player targetPlayer = context.getOrDefault("target", null);
        if (targetPlayer == null) return;

        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(sender.getUniqueId());
        GrimPlayer target = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(targetPlayer.getUniqueId());

        if (target != null && target.getUniqueId().equals(player.getUniqueId())) {
            String message = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("cannot-run-on-self", "%prefix% &cYou cannot use this command on yourself!");
            message = MessageUtil.replacePlaceholders(target, message);
            player.sendMessage(MessageUtil.miniMessage(message));
            return;
        }

        if (target == null || (target.platformPlayer != null && target.platformPlayer.isExternalPlayer())) {
            String message = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("player-not-this-server", "%prefix% &cThis player isn't on this server!");
            message = MessageUtil.replacePlaceholders(target, message);
            player.sendMessage(MessageUtil.miniMessage(message));
            return;
        }

        // hide player from tab list
        if (GrimAPI.INSTANCE.getSpectateManager().enable(player)) {
            String message = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("spectate-return", "<click:run_command:/grim stopspectating><hover:show_text:\"/grim stopspectating\">\n%prefix% &fClick here to return to previous location\n</hover></click>");
            message = MessageUtil.replacePlaceholders(target, message);
            player.user.sendMessage(MessageUtil.miniMessage(message));
        }

        player.setGameMode(GameMode.SPECTATOR);
        player.platformPlayer.teleportAsync(target.getLocation());
    }
}
