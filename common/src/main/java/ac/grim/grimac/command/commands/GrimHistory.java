package ac.grim.grimac.command.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.manager.violationdatabase.Violation;
import ac.grim.grimac.manager.violationdatabase.ViolationDatabaseManager;
import ac.grim.grimac.platform.api.manager.cloud.CloudCommandAdapter;
import ac.grim.grimac.platform.api.player.OfflinePlatformPlayer;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringParser;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class GrimHistory implements BuildableCommand {

    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
                commandManager.commandBuilder("grim", "grimac")
                        .literal("history", "hist")
                        .permission("grim.help")
                        .required("target", StringParser.stringParser(), adapter.onlinePlayerSuggestions())
                        .optional("page", IntegerParser.integerParser())
                        .permission("grim.history")
                        .handler(this::handleHistory)
        );
    }

    private void handleHistory(CommandContext<Sender> context) {
        Sender sender = context.sender();
        String target = context.get("target");
        Integer page = context.getOrDefault("page", 1);

        if (!GrimAPI.INSTANCE.getViolationDatabaseManager().isEnabled()) {
            String msg = GrimAPI.INSTANCE.getConfigManager().getConfig()
                    .getStringElse("grim-history-disabled",
                            "%prefix% &cHistory subsystem is disabled!");
            sender.sendMessage(MessageUtil.miniMessage(msg));
            return;
        } else if (!GrimAPI.INSTANCE.getViolationDatabaseManager().isLoaded()) {
            String msg = GrimAPI.INSTANCE.getConfigManager().getConfig()
                    .getStringElse("grim-history-load-failure",
                            "%prefix% &cHistory subsystem failed to load! Check server console for errors.");
            sender.sendMessage(MessageUtil.miniMessage(msg));
            return;
        }

        GrimAPI.INSTANCE.getScheduler().getAsyncScheduler().runNow(GrimAPI.INSTANCE.getGrimPlugin(), () -> {
            int entriesPerPage = GrimAPI.INSTANCE.getConfigManager().getConfig().getIntElse("history.entries-per-page", 15);
            String header = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("grim-history-header",
                    "%prefix% &bShowing logs for &f%player% (&f%page%&b/&f%maxPages%&f)");
            String logFormat = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("grim-history-entry",
                    "%prefix% &8[&f%server%&8] &bFailed &f%check% (x&c%vl%&f) &7%verbose% (&b%timeago% ago&7)");

            OfflinePlatformPlayer targetPlayer = GrimAPI.INSTANCE.getPlatformPlayerFactory().getOfflineFromName(target);

            ViolationDatabaseManager violations = GrimAPI.INSTANCE.getViolationDatabaseManager();
            int logCount = violations.getLogCount(targetPlayer.getUniqueId());
            List<Violation> logs = violations.getViolations(targetPlayer.getUniqueId(), page, entriesPerPage);
            int maxPages = (int) Math.ceil((float) logCount / entriesPerPage);

            sender.sendMessage(MessageUtil.miniMessage(MessageUtil.replacePlaceholders(sender, header
                    .replace("%player%", targetPlayer.getName())
                    .replace("%page%", String.valueOf(page))
                    .replace("%maxPages%", String.valueOf(maxPages))
            )));

            for (int i = logs.size() - 1; i >= 0; i--) {
                Violation log = logs.get(i);
                sender.sendMessage(MessageUtil.miniMessage(MessageUtil.replacePlaceholders(sender, logFormat
                        .replace("%player%", targetPlayer.getName())
                        .replace("%grim_version%", log.grimVersion())
                        .replace("%client_brand%", log.clientBrand())
                        .replace("%client_version%", log.clientVersion())
                        .replace("%server_version%", log.serverVersion())
                        .replace("%check%", log.checkName())
                        .replace("%verbose%", log.verbose())
                        .replace("%vl%", String.valueOf(log.vl()))
                        .replace("%timeago%", getTimeAgo(log.createdAt()))
                        .replace("%server%", log.server())
                )));
            }
        });
    }

    /**
     * Calculates the time elapsed since a given timestamp in a human-readable format.
     *
     * @param timestamp The timestamp in milliseconds since epoch (e.g., from System.currentTimeMillis()).
     * @return A string representing the time elapsed (e.g., "5d 3h 10m").
     */
    private String getTimeAgo(long timestamp) {
        // Calculate duration directly from current time and the provided timestamp
        long durationMillis = System.currentTimeMillis() - timestamp;

        // Ensure duration is non-negative, though for "time ago" it should be.
        if (durationMillis < 0) {
            return "0s"; // Or handle as an error/future time
        }

        long days = TimeUnit.MILLISECONDS.toDays(durationMillis);
        durationMillis -= TimeUnit.DAYS.toMillis(days);

        long hours = TimeUnit.MILLISECONDS.toHours(durationMillis);
        durationMillis -= TimeUnit.HOURS.toMillis(hours);

        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis);
        durationMillis -= TimeUnit.MINUTES.toMillis(minutes);

        long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis);

        StringBuilder result = new StringBuilder();
        if (days > 0) result.append(days).append("d ");
        if (hours > 0) result.append(hours).append("h ");
        if (minutes > 0) result.append(minutes).append("m ");
        if (seconds > 0 || result.isEmpty()) result.append(seconds).append("s"); // Always show seconds if nothing else, or if it's 0s.

        return result.toString().trim();
    }
}
