package ac.grim.grimac.command.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.GrimIdentity;
import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.command.CommandUtils;
import ac.grim.grimac.platform.api.manager.cloud.CloudCommandAdapter;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.player.GrimPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GrimList implements BuildableCommand {

    // Mainly for debugging purposes. Useful for seeing which players are exempt or not.

    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(commandManager.commandBuilder("grim", "grimac")
                .literal("list")
                .permission("grim.list")
                .required("list", StringParser.stringParser(), SUGGESTIONS)
                .handler(commandContext -> handleList(commandContext.sender(), commandContext.getOrDefault("list", "?").toLowerCase()))
                .build());
    }

    private final SuggestionProvider<Sender> SUGGESTIONS = CommandUtils.fromStrings("players");

    private void handleList(Sender sender, String id) {
        switch (id) {
            case "players" -> handleListPlayers(sender);
            default -> sender.sendMessage(Component.text()
                    .append(Component.text("Invalid argument: ", NamedTextColor.GRAY))
                    .append(Component.text(id, NamedTextColor.RED))
                    .build());
        }
    }

    private Component playerComponent(String name, UUID uuid, boolean online, boolean exempt) {
        return Component.text(name)
                .color(exempt ? (online ? NamedTextColor.GRAY : NamedTextColor.DARK_GRAY)
                        : (online ? NamedTextColor.WHITE : NamedTextColor.RED))
                .clickEvent(ClickEvent.copyToClipboard(name))
                .hoverEvent(HoverEvent.showText(playerHoverComponent(uuid, online, exempt, true)));
    }

    private Component playerHoverComponent(UUID uuid, boolean online, boolean exempt, boolean registered) {
        var builder = Component.text();
        builder.append(Component.text()
                .append(Component.text("UUID: ").color(NamedTextColor.GRAY))
                .append(Component.text(uuid + "").color(NamedTextColor.WHITE))
                .append(Component.newline())
                .append(Component.text("Status: ").color(NamedTextColor.GRAY))
                .append(online ? Component.text("Online").color(NamedTextColor.GREEN)
                        : Component.text("Offline").color(NamedTextColor.RED)));
        if (exempt) {
            builder.append(Component.newline());
            builder.append(Component.text("Is Exempt").color(NamedTextColor.LIGHT_PURPLE));
        }
        if (!registered) {
            builder.append(Component.newline());
            builder.append(Component.text("Not Registered").color(NamedTextColor.RED));
        }
        return builder.build();
    }

    private void handleListPlayers(Sender sender) {
        final TextComponent.Builder builder = Component.text();

        final Map<UUID, PlatformPlayer> onlinePlayers = GrimAPI.INSTANCE.getPlatformPlayerFactory().getOnlinePlayers()
                .stream().collect(Collectors.toMap(GrimIdentity::getUniqueId, Function.identity()));
        //
        final Set<PlatformPlayer> unregisteredPlayers = new HashSet<>(onlinePlayers.values());

        boolean after = false;
        builder.append(Component.text("Players = [", NamedTextColor.GRAY));
        //
        for (GrimPlayer entry : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            if (after) {
                builder.append(Component.text(", ").color(NamedTextColor.GRAY));
            } else {
                after = true;
            }
            PlatformPlayer platformPlayer = onlinePlayers.get(entry.getUniqueId());
            if (platformPlayer != null) unregisteredPlayers.remove(platformPlayer);
            boolean online = platformPlayer != null && platformPlayer.isOnline();
            boolean exempt = !GrimAPI.INSTANCE.getPlayerDataManager().shouldCheck(entry.user);
            builder.append(playerComponent(entry.getName(), entry.getUniqueId(), online, exempt));
        }
        //
        for (PlatformPlayer platformPlayer : unregisteredPlayers) {
            if (after) {
                builder.append(Component.text(", ").color(NamedTextColor.GRAY));
            } else {
                after = true;
            }
            builder.append(Component.text(platformPlayer.getName()).color(NamedTextColor.LIGHT_PURPLE)
                    .clickEvent(ClickEvent.suggestCommand(platformPlayer.getName()))
                    .hoverEvent(HoverEvent.showText(playerHoverComponent(platformPlayer.getUniqueId(), platformPlayer.isOnline(), false, false)))
            );
        }
        // close and send
        builder.append(Component.text("]", NamedTextColor.GRAY));
        sender.sendMessage(builder.build());
    }

}
