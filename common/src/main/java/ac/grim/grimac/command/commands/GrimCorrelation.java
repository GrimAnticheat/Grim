package ac.grim.grimac.command.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.manager.ViolationCorrelationTracker;
import ac.grim.grimac.platform.api.command.PlayerSelector;
import ac.grim.grimac.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.player.GrimPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public class GrimCorrelation implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        commandManager.command(commandManager.commandBuilder("grim", "grimac")
                .literal("correlation")
                .permission("grim.correlation")
                .required("target", arguments.singlePlayerSelectorParser())
                .handler(this::handleCorrelation));
    }

    private void handleCorrelation(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        PlayerSelector selector = context.get("target");
        Sender target = selector.getSinglePlayer();
        if (target.getPlatformPlayer() == null || target.getPlatformPlayer().isExternalPlayer()) {
            sender.sendMessage(Component.text("Player is not on this server.", NamedTextColor.RED));
            return;
        }

        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(target.getUniqueId());
        if (player == null) {
            sender.sendMessage(Component.text("Player is exempt or not tracked by Grim.", NamedTextColor.RED));
            return;
        }

        ViolationCorrelationTracker.Snapshot snapshot = player.violationCorrelationTracker.snapshot();
        sender.sendMessage(Component.text("Recent check signals for " + player.getName()
                + " (10s window, observe-only): " + snapshot.totalSignals()
                + " signal(s) across " + snapshot.familyCount() + " family/families.",
                NamedTextColor.GRAY));
        if (snapshot.totalSignals() == 0) {
            sender.sendMessage(Component.text("No recent correlation signals.", NamedTextColor.DARK_GRAY));
            return;
        }

        for (ViolationCorrelationTracker.Family family : ViolationCorrelationTracker.Family.values()) {
            Integer count = snapshot.counts().get(family);
            if (count == null) continue;
            Set<String> checks = snapshot.checks().getOrDefault(family, Set.of());
            sender.sendMessage(Component.text(family.name().toLowerCase()
                    + ": " + count + " signal(s) from " + checks.size() + " check(s)", NamedTextColor.GRAY));
        }
        sender.sendMessage(Component.text("This report does not affect VL, setbacks, or punishments.",
                NamedTextColor.DARK_GRAY));
    }
}
