package ac.grim.grimac.command.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.platform.api.sender.Sender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
        );
    }

    private void handleVerbose(@NonNull CommandContext<Sender> context) {
        Sender sender = context.sender();

        if (!sender.isPlayer()) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return;
        }

        GrimAPI.INSTANCE.getAlertManager().toggleVerbose(sender.getPlatformPlayer());
    }
}
