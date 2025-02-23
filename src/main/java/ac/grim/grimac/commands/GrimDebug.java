package ac.grim.grimac.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.api.command.PlayerSelector;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.platform.api.sender.Sender;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.parser.standard.StringParser;

import java.util.UUID;

public class GrimDebug implements BuildableCommand {

    public void register(CommandManager<Sender> commandManager) {
        Command.Builder<Sender> grimCommand = commandManager.commandBuilder("grim", "grimac");

        // Register "debug" subcommand
        Command.Builder<Sender> debugCommand = grimCommand
                .literal("debug", Description.of("Toggle debug output for a player"))
                .permission("grim.debug")
                .optional("target", GrimAPI.INSTANCE.getParserDescriptors().getSinglePlayer())
                .handler(this::handleDebug);

        // Register "consoledebug" subcommand
        Command.Builder<Sender> consoleDebugCommand = grimCommand
                .literal("consoledebug", Description.of("Toggle console debug output for a player"))
                .permission("grim.consoledebug")
                .optional("target", GrimAPI.INSTANCE.getParserDescriptors().getSinglePlayer())
                .handler(this::handleConsoleDebug);

        // Register commands
        commandManager.command(debugCommand);
        commandManager.command(consoleDebugCommand);

        // Register completions for players
//        commandManager.parserRegistry().registerSuggestionProvider("players", (context, input) -> {
//            // Abstract player name suggestions (cross-platform)
//            return GrimAPI.INSTANCE.getOnlinePlayerNames().stream()
//                    .filter(name -> name.toLowerCase().startsWith(input.toLowerCase()))
//                    .collect(Collectors.toList());
//        });
    }

    private void handleDebug(@NonNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        PlayerSelector playerSelector = context.getOrDefault("target", null);

        GrimPlayer grimPlayer = parseTarget(sender, playerSelector.getSinglePlayer());
        if (grimPlayer == null) return;

        if (sender.isConsole()) {
            grimPlayer.checkManager.getDebugHandler().toggleConsoleOutput();
        } else {
            GrimPlayer senderGrimPlayer = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(sender.getUniqueId());
            grimPlayer.checkManager.getDebugHandler().toggleListener(senderGrimPlayer);
        }
    }

    private void handleConsoleDebug(@NonNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        PlayerSelector targetName = context.getOrDefault("target", null);

        GrimPlayer grimPlayer = parseTarget(sender, targetName.getSinglePlayer());
        if (grimPlayer == null) return;

        boolean isOutput = grimPlayer.checkManager.getDebugHandler().toggleConsoleOutput();
        String playerName = grimPlayer.user.getProfile().getName(); // Use user profile for name

        Component message = Component.text()
                .append(Component.text("Console output for ", NamedTextColor.GRAY))
                .append(Component.text(playerName, NamedTextColor.WHITE))
                .append(Component.text(" is now ", NamedTextColor.GRAY))
                .append(Component.text(isOutput ? "enabled" : "disabled", NamedTextColor.WHITE))
                .build();

        sender.sendMessage(message);
    }

    private @Nullable GrimPlayer parseTarget(@NonNull Sender sender, @Nullable Sender t) {
        if (sender.isConsole() && t == null) {
            sender.sendMessage(Component.text("You must specify a target as the console!", NamedTextColor.RED));
            return null;
        }
        Sender target = t == null ? sender : t;

        GrimPlayer grimPlayer = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(target.getUniqueId());
        if (grimPlayer == null) {
            User user = PacketEvents.getAPI().getPlayerManager().getUser(target.getSender());
            sender.sendMessage(Component.text("This player is exempt from all checks!", NamedTextColor.RED));

            if (user == null) {
                sender.sendMessage(Component.text("Unknown PacketEvents user", NamedTextColor.RED));
            } else {
                boolean isExempt = GrimAPI.INSTANCE.getPlayerDataManager().shouldCheck(user);
                if (!isExempt) {
                    sender.sendMessage(Component.text("User connection state: " + user.getConnectionState(), NamedTextColor.RED));
                }
            }
        }

        return grimPlayer;
    }
}
