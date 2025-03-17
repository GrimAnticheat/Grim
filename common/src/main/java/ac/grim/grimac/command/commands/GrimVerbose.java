package ac.grim.grimac.command.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.command.requirements.PlayerSenderRequirement;
import ac.grim.grimac.manager.init.start.CommandRegister;
import ac.grim.grimac.platform.api.sender.Sender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;

public class GrimVerbose implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager) {
        commandManager.command(
                commandManager.commandBuilder("grim", "grimac")
                        .literal("verbose")
                        .permission("grim.verbose")
                        .handler(this::handleVerbose)
                        .apply(CommandRegister.REQUIREMENT_FACTORY.create(PlayerSenderRequirement.PLAYER_SENDER_REQUIREMENT))
        );
    }

    private void handleVerbose(@NonNull CommandContext<Sender> context) {
        GrimAPI.INSTANCE.getAlertManager().toggleVerbose(context.sender().getPlatformPlayer());
    }
}
