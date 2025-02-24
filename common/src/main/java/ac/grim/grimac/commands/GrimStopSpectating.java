package ac.grim.grimac.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.List;

public class GrimStopSpectating implements BuildableCommand {

    @Override
    public void register(CommandManager<Sender> commandManager) {
        commandManager.command(
                commandManager.commandBuilder("grim", "grimac")
                        .literal("stopspectating")
                        .permission("grim.spectate")
                        .optional("here", StringParser.stringParser(), SuggestionProvider.blocking((ctx, in) -> {
                            if (ctx.sender().hasPermission("grim.spectate.stophere")) {
                                return List.of(Suggestion.suggestion("here"));
                            }
                            return List.of(); // No suggestions if no permission
                        }))
//                        .suggester((context, input) -> {
//                            return Bukkit.getOnlinePlayers().stream()
//                                    .map(Player::getName)
//                                    .filter(name -> input.isEmpty() || name.toLowerCase().startsWith(input.toLowerCase()))
//                                    .collect(Collectors.toList());
//                        })
                        .handler(this::onStopSpectate)
        );
    }

    public void onStopSpectate(CommandContext<Sender> commandContext) {
//        if (!(sender instanceof Player player)) return;
        Sender sender = commandContext.sender();
        if (sender.isConsole()) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return;
        }

        String string = commandContext.getOrDefault("here", null);
        if (GrimAPI.INSTANCE.getSpectateManager().isSpectating(sender.getUniqueId())) {
            boolean teleportBack = string == null || !string.equalsIgnoreCase("here") || !sender.hasPermission("grim.spectate.stophere");
            GrimAPI.INSTANCE.getSpectateManager().disable(GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(sender.getUniqueId()), teleportBack);
        } else {
            String message = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("cannot-spectate-return", "%prefix% &cYou can only do this after spectating a player.");
            message = MessageUtil.replacePlaceholders(sender, message);
            sender.sendMessage(MessageUtil.miniMessage(message));
        }
    }
}
