package ac.grim.grimac.command.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.command.CloudCommandService;
import ac.grim.grimac.command.requirements.PlayerSenderRequirement;
import ac.grim.grimac.platform.api.manager.cloud.CloudCommandAdapter;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.List;
import java.util.Objects;

public class GrimStopSpectating implements BuildableCommand {

    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
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
                        .handler(this::onStopSpectate)
                        .apply(CloudCommandService.REQUIREMENT_FACTORY.create(PlayerSenderRequirement.PLAYER_SENDER_REQUIREMENT))
        );
    }

    public void onStopSpectate(CommandContext<Sender> commandContext) {
        Sender sender = commandContext.sender();
        String string = commandContext.getOrDefault("here", null);
        if (GrimAPI.INSTANCE.getSpectateManager().isSpectating(sender.getUniqueId())) {
            boolean teleportBack = string == null || !string.equalsIgnoreCase("here") || !sender.hasPermission("grim.spectate.stophere");
            GrimAPI.INSTANCE.getSpectateManager().disable(Objects.requireNonNull(sender.getPlatformPlayer()), teleportBack);
        } else {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "cannot-spectate-return", "%prefix% &cYou can only do this after spectating a player."));
        }
    }
}
