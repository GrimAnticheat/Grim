package ac.grim.grimac.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.api.command.PlayerSelector;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.api.sender.Sender;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

        if (sender.isConsole()) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return;
        }

        PlayerSelector targetSelectorResults = context.getOrDefault("target", null);
        if (targetSelectorResults == null) return;

        PlatformPlayer targetPlatformPlayer = targetSelectorResults.getSinglePlayer().getPlatformPlayer();

        if (targetPlatformPlayer != null && targetPlatformPlayer.getUniqueId().equals(sender.getUniqueId())) {
            String message = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("cannot-run-on-self", "%prefix% &cYou cannot use this command on yourself!");
            message = MessageUtil.replacePlaceholders(targetPlatformPlayer, message);
            sender.sendMessage(MessageUtil.miniMessage(message));
            return;
        }

        if (targetPlatformPlayer != null && targetPlatformPlayer.isExternalPlayer()) {
            String message = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("player-not-this-server", "%prefix% &cThis player isn't on this server!");
            message = MessageUtil.replacePlaceholders(targetPlatformPlayer, message);
            sender.sendMessage(MessageUtil.miniMessage(message));
            return;
        }

        @NonNull PlatformPlayer platformPlayer = sender.getPlatformPlayer();

        // hide player from tab list
        if (GrimAPI.INSTANCE.getSpectateManager().enable(platformPlayer)) {
            String message = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("spectate-return", "<click:run_command:/grim stopspectating><hover:show_text:\"/grim stopspectating\">\n%prefix% &fClick here to return to previous location\n</hover></click>");
            message = MessageUtil.replacePlaceholders(targetPlatformPlayer, message);
            sender.sendMessage(MessageUtil.miniMessage(message));
        }

        platformPlayer.setGameMode(GameMode.SPECTATOR);
        platformPlayer.teleportAsync(targetPlatformPlayer.getLocation());
    }
}
