package ac.grim.grimac.command.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.platform.api.sender.Sender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;

public class GrimAlerts implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager) {
        commandManager.command(
                commandManager.commandBuilder("grim", "grimac")
                        .literal("alerts", Description.of("Toggle alerts for the sender"))
                        .permission("grim.alerts")
                        .handler(this::handleAlerts)
        );
    }

    @SuppressWarnings("ConstantConditions")
    // Suppress warning as we've already checked sender is not console
    private void handleAlerts(@NonNull CommandContext<Sender> context) {
        Sender sender = context.sender();

        // Ensure the sender is a player (console not supported)
        if (!sender.isPlayer()) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return;
        }

        GrimAPI.INSTANCE.getAlertManager().toggleAlerts(sender.getPlatformPlayer());
    }
}
