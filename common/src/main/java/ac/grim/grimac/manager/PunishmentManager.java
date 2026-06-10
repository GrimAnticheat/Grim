package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.config.ConfigReloadable;
import ac.grim.grimac.api.event.events.CommandExecuteEvent;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.events.packets.ProxyAlertMessenger;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public class PunishmentManager implements ConfigReloadable {
    private static final CommandExecuteEvent.Channel COMMAND_CHANNEL = GrimAPI.INSTANCE.getEventBus().get(CommandExecuteEvent.class);
    private final GrimPlayer player;
    private final List<PunishGroup> groups = new ArrayList<>();
    private String experimentalSymbol = "*";
    private String alertString;
    private boolean testMode;
    private String proxyAlertString = "";

    public PunishmentManager(GrimPlayer player) {
        this.player = player;
    }

    @Override
    public void reload(ConfigManager config) {
        List<String> punish = config.getStringListElse("Punishments", new ArrayList<>());
        experimentalSymbol = config.getStringElse("experimental-symbol", "*");

        alertString = config.getStringElse(
                "alerts-format",
                "%prefix% &f%player% &bfailed <hover:show_text:\"&b%check_name%%experimental%\\n&8Description: &f%description%\">&f%check_name%%experimental%</hover> &f(x&c%vl%&f) &7%verbose%"
        );

        testMode = config.getBooleanElse("test-mode", false);

        proxyAlertString = config.getStringElse(
                "alerts-format-proxy",
                "%prefix% &f[&cproxy&f] &f%player% &bfailed <hover:show_text:\"&b%check_name%%experimental%\\n&8Description: &f%description%\">&f%check_name%%experimental%</hover> &f(x&c%vl%&f) &7%verbose%"
        );

        try {
            groups.clear();

            // To support reloading
            for (AbstractCheck check : player.checkManager.allChecks.values()) {
                check.setEnabled(false);
            }

            for (Object s : punish) {
                LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) s;

                List<String> checks = (List<String>) map.getOrDefault("checks", new ArrayList<>());
                List<String> commands = (List<String>) map.getOrDefault("commands", new ArrayList<>());
                int removeViolationsAfter = (int) map.getOrDefault("remove-violations-after", 300);

                List<ParsedCommand> parsed = new ArrayList<>();
                List<AbstractCheck> checksList = new ArrayList<>();
                List<AbstractCheck> excluded = new ArrayList<>();
                for (String command : checks) {
                    command = command.toLowerCase(Locale.ROOT);
                    boolean exclude = false;
                    if (command.startsWith("!")) {
                        exclude = true;
                        command = command.substring(1);
                    }
                    for (AbstractCheck check : player.checkManager.allChecks.values()) { // o(n) * o(n)?
                        if (check.getCheckName() != null &&
                                (check.getCheckName().toLowerCase(Locale.ROOT).contains(command)
                                        || check.getAlternativeName().toLowerCase(Locale.ROOT).contains(command))) { // Some checks have equivalent names like AntiKB and AntiKnockback
                            if (exclude) {
                                excluded.add(check);
                            } else {
                                checksList.add(check);
                                check.setEnabled(true);
                            }
                        }
                    }
                    for (AbstractCheck check : excluded) checksList.remove(check);
                }

                for (String command : commands) {
                    String firstNum = command.substring(0, command.indexOf(":"));
                    String secondNum = command.substring(command.indexOf(":"), command.indexOf(" "));

                    int threshold = Integer.parseInt(firstNum);
                    int interval = Integer.parseInt(secondNum.substring(1));
                    String commandString = command.substring(command.indexOf(" ") + 1);

                    parsed.add(new ParsedCommand(threshold, interval, commandString));
                }

                groups.add(new PunishGroup(checksList, parsed, removeViolationsAfter * 1000));
            }
        } catch (Exception e) {
            LogUtil.error("Error while loading punishments.yml! This is likely your fault!", e);
        }
    }

    private String replaceAlertPlaceholders(String original, int vl, Check check, String verbose) {
        return MessageUtil.replacePlaceholders(player, original
                .replace("[alert]", alertString)
                .replace("[proxy]", proxyAlertString)
                .replace("%check_name%", check.getDisplayName())
                .replace("%experimental%", check.isExperimental() ? experimentalSymbol : "")
                .replace("%vl%", Integer.toString(vl))
                .replace("%description%", check.getDescription())
                .replace("%stable_key%", check.getStableKey())
        ).replace("%verbose%", MessageUtil.miniMessageSafe(verbose));
    }

    public boolean handleAlert(GrimPlayer player, String verbose, Check check) {
        String value = verbose == null ? "" : verbose;
        return handleAlert(player, () -> value, check);
    }

    public boolean handleAlert(GrimPlayer player, Supplier<String> verbose, Check check) {
        boolean sentDebug = false;

        // Check commands
        for (PunishGroup group : groups) {
            if (group.checks.contains(check)) {
                final int vl = getViolations(group, check);
                final int violationCount = group.violations.size();
                for (ParsedCommand command : group.commands) {
                    @Nullable Set<@Nullable PlatformPlayer> verboseListeners = null;
                    // Verbose that prints all flags: /grim verbose subscribers get EVERY flag, not just thresholded
                    // alerts. The verbose string is only rendered (safeGet) when there are listeners, so the flag
                    // path stays lazy when nobody is subscribed.
                    if (command.command.equals("[alert]") && GrimAPI.INSTANCE.getAlertManager().hasVerboseListeners()) {
                        sentDebug = true;
                        String verboseForListeners = safeGet(verbose);
                        String listenerCmd = replaceAlertPlaceholders(command.command, vl, check, verboseForListeners);
                        verboseListeners = GrimAPI.INSTANCE.getAlertManager().sendVerbose(MessageUtil.miniMessage(listenerCmd), null);
                    }
                    if (violationCount >= command.threshold) {
                        boolean shouldRun = command.interval == 0
                                ? command.executeCount == 0
                                : violationCount >= command.nextBoundary;
                        if (shouldRun) {
                            String renderedVerbose = safeGet(verbose);
                            String cmd = replaceAlertPlaceholders(command.command, vl, check, renderedVerbose);
                            boolean canceled = COMMAND_CHANNEL.fire(player, check, renderedVerbose, cmd);
                            if (command.interval == 0) {
                                command.executeCount++;
                            } else {
                                advanceBoundary(command, violationCount);
                            }
                            if (canceled) continue;

                            switch (command.command) {
                                case "[webhook]" -> GrimAPI.INSTANCE.getDiscordManager().sendAlert(player, renderedVerbose, check.getDisplayName(), vl);
                                case "[log]" -> {
                                    // Binary-verbose checks store from flag(VerboseBuf); avoid an extra legacy text row.
                                    if (!usesStructuredVerbose(check)) {
                                        String verboseWithoutGl = renderedVerbose.replaceAll(" /gl .*", "");
                                        GrimAPI.INSTANCE.getDataStoreLifecycle().liveWriteHooks()
                                                .recordFlagFromCheck(player, check, vl, verboseWithoutGl);
                                    }
                                }
                                case "[proxy]" -> ProxyAlertMessenger.sendPluginMessage(cmd);
                                case "[alert]" -> {
                                    sentDebug = true;
                                    Component message = MessageUtil.miniMessage(cmd);
                                    if (testMode) { // secret test mode
                                        if (verboseListeners == null || verboseListeners.contains(player.platformPlayer)) {
                                            player.sendMessage(message);
                                        }
                                    } else {
                                        GrimAPI.INSTANCE.getAlertManager().sendAlert(message, verboseListeners);
                                    }
                                }
                                default -> GrimAPI.INSTANCE.getScheduler().getGlobalRegionScheduler().run(GrimAPI.INSTANCE.getGrimPlugin(), () ->
                                        GrimAPI.INSTANCE.getPlatformServer().dispatchCommand(
                                                GrimAPI.INSTANCE.getPlatformServer().getConsoleSender(),
                                                cmd
                                        )
                                );
                            }
                        }

                        if (command.interval > 0) command.executeCount++;
                    } else {
                        // Interval commands re-arm after the active rolling count
                        // cools below their threshold.
                        command.nextBoundary = command.threshold;
                    }
                }
            }
        }

        return sentDebug;
    }

    private static String safeGet(Supplier<String> supplier) {
        try {
            String value = supplier.get();
            return value == null ? "" : value;
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static boolean usesStructuredVerbose(Check check) {
        CheckData data = check.getClass().getAnnotation(CheckData.class);
        return data != null && data.verboseVersion() >= 1;
    }

    private static void advanceBoundary(ParsedCommand command, int violationCount) {
        command.nextBoundary += ((violationCount - command.nextBoundary) / command.interval + 1) * command.interval;
    }

    public void handleViolation(Check check) {
        for (PunishGroup group : groups) {
            if (group.checks.contains(check)) {
                long currentTime = System.currentTimeMillis();

                group.violations.put(currentTime, check);
                // Remove violations older than the defined time in the config
                group.violations.long2ObjectEntrySet().removeIf(time -> currentTime - time.getLongKey() > group.removeViolationsAfter);
            }
        }
    }

    private int getViolations(PunishGroup group, Check check) {
        int vl = 0;
        for (Check value : group.violations.values()) {
            if (value == check) vl++;
        }
        return vl;
    }
}

@RequiredArgsConstructor
class PunishGroup {
    public final List<AbstractCheck> checks;
    public final List<ParsedCommand> commands;
    public final Long2ObjectMap<Check> violations = new Long2ObjectOpenHashMap<>();
    public final int removeViolationsAfter; // time to remove violations after in milliseconds
}

class ParsedCommand {
    public final int threshold;
    public final int interval;
    public final String command;
    // Legacy M=0 gate: execute once for this loaded command state.
    public int executeCount;
    // For M>0, the next active violation count that should run this command.
    public int nextBoundary;

    public ParsedCommand(int threshold, int interval, String command) {
        this.threshold = threshold;
        this.interval = interval;
        this.command = command;
        this.nextBoundary = threshold;
    }
}
