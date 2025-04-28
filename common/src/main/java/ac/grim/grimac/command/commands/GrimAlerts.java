package ac.grim.grimac.command.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.platform.api.sender.Sender;
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

    // Suppress warning as we've already checked sender is not console
    private void handleAlerts(@NonNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (sender.isPlayer()) {
            GrimAPI.INSTANCE.getAlertManager().toggleAlerts(context.sender().getPlatformPlayer(), false);
        } else if (sender.isConsole()) {
            GrimAPI.INSTANCE.getAlertManager().toggleConsoleAlerts();
        }
    }
}
