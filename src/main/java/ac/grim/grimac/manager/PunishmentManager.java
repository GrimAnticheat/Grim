package ac.grim.grimac.manager;

import ac.grim.grimac.AbstractCheck;
import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.CachedCheck;
import ac.grim.grimac.events.CommandExecuteEvent;
import ac.grim.grimac.events.packets.ProxyAlertMessenger;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import github.scarsz.configuralize.DynamicConfig;
import io.github.retrooper.packetevents.util.FoliaCompatUtil;
import lombok.Builder;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

public class PunishmentManager implements Initable {
    @Getter
    private List<PunishGroup> groups;
    @Getter
    private boolean violated;
    private static final List<CachedPunishGroup> cachedPunishGroups = new ArrayList<>();

    private static final Set<Class<? extends AbstractCheck>> disabledChecks = new HashSet<>();


    public PunishmentManager() {
        //API
    }

    public PunishmentManager(GrimPlayer grimPlayer) {
        groups = new ArrayList<>();
        for (CachedPunishGroup cachedPunishGroup : cachedPunishGroups) {
            groups.add(new PunishGroup(cachedPunishGroup));
        }
    }

    public void cleanup() {
        getGroups().forEach(PunishGroup::cleanup);
    }

    public void reloadCachedGroups() {
        groups.clear();
        for (CachedPunishGroup cachedPunishGroup : cachedPunishGroups) {
            groups.add(new PunishGroup(cachedPunishGroup));
        }
    }

    @Override
    public void start() {
        reload();
        //Violations cleaner
        Bukkit.getScheduler().runTaskTimerAsynchronously(GrimAPI.INSTANCE.getPlugin(),
                () -> {
                    GrimAPI.INSTANCE.getPlayerDataManager()
                            .getEntries().forEach(grimPlayer -> {
                                PunishmentManager punishmentManager = grimPlayer.getPunishmentManager();
                                if (punishmentManager.isViolated()) punishmentManager.cleanup();
                            });
                }, 200, 200);
    }

    public void reload() {
        DynamicConfig config = GrimAPI.INSTANCE.getConfigManager().getConfig();
        List<String> punish = config.getStringListElse("Punishments", new ArrayList<>());

        try {
            disabledChecks.clear();
            cachedPunishGroups.clear();

            // To support reloading

            for (Object s : punish) {
                LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) s;

                List<String> checks = (List<String>) map.getOrDefault("checks", new ArrayList<>());
                List<String> commands = (List<String>) map.getOrDefault("commands", new ArrayList<>());
                int removeViolationsAfter = (int) map.getOrDefault("remove-violations-after", 300);

                List<ParsedCommand> parsed = new ArrayList<>();
                List<Class<? extends AbstractCheck>> checksList = new ArrayList<>();
                List<Class<? extends AbstractCheck>> excluded = new ArrayList<>();
                for (String checkString : checks) {
                    checkString = checkString.toLowerCase(Locale.ROOT);
                    boolean exclude = false;
                    if (checkString.startsWith("!")) {
                        exclude = true;
                        checkString = checkString.substring(1);
                    }
                    Iterator<Map.Entry<Class<? extends AbstractCheck>,
                            CachedCheck<?>>> iterator =
                            CheckManager.cachedAllChecks.entrySet().iterator();
                    boolean correctName = false;
                    while (iterator.hasNext()) {
                        Map.Entry<Class<? extends AbstractCheck>,
                                CachedCheck<? extends AbstractCheck>> entry = iterator.next();
                        Class<? extends AbstractCheck> aClass = entry.getKey();
                        Class<? extends AbstractCheck> resultCheck = null;
                        if (aClass.isAnnotationPresent(CheckData.class)) {
                            final CheckData checkData = aClass.getAnnotation(CheckData.class);
                            String checkName = checkData.name(), alternativeName = checkData.alternativeName(),
                                    configName = checkData.configName();
                            if (checkName != null && (checkName.toLowerCase().contains(checkString) ||
                                    alternativeName.toLowerCase().contains(checkString) ||
                                    alternativeName.toLowerCase().contains(configName))) {
                                resultCheck = aClass;
                                correctName = true;
                                if (exclude) {
                                    disabledChecks.add(resultCheck);
                                    excluded.add(resultCheck);
                                    GrimAPI.INSTANCE.getAlertManager().getEnabledAlerts().forEach(player ->
                                            player.sendMessage("Config: " + ChatColor.RED + "disabled " + checkName));
                                } else {
                                    checksList.add(resultCheck);
                                }
                                for (Class<? extends AbstractCheck> check : excluded) checksList.remove(check);
                            }
                        }
                    }
                    if (!correctName) {
                        String finalCheckString = checkString;
                        GrimAPI.INSTANCE.getAlertManager().getEnabledAlerts().forEach(player ->
                                player.sendMessage(ChatColor.RED + "Incorrect config Check: " + finalCheckString));
                    }
                }
                for (String command : commands) {
                    String firstNum = command.substring(0, command.indexOf(":"));
                    String secondNum = command.substring(command.indexOf(":"), command.indexOf(" "));

                    int threshold = Integer.parseInt(firstNum);
                    int interval = Integer.parseInt(secondNum.substring(1));
                    String commandString = command.substring(command.indexOf(" ") + 1);

                    parsed.add(new ParsedCommand(threshold, interval, commandString));
                }
                cachedPunishGroups.add(CachedPunishGroup.builder()
                        .checks(checksList)
                        .commands(parsed)
                        .removeViolationsAfter(removeViolationsAfter * 1000)
                        .build());
            }
            GrimAPI.INSTANCE.getAlertManager().getEnabledAlerts().forEach(player ->
                    player.sendMessage(ChatColor.GREEN + "Cached groups: " + cachedPunishGroups.size()));
            CheckManager.cachedAllChecks.forEach(((aClass, cachedCheck) -> {
                cachedCheck.setDisabled(disabledChecks.contains(aClass));
            }));
        } catch (Exception e) {
            LogUtil.error("Error while loading punishments.yml! This is likely your fault!");
            e.printStackTrace();
        }
    }

    private String replaceAlertPlaceholders(GrimPlayer player, String original, PunishGroup group, Check check, String alertString, String verbose) {
        // Streams are slow but this isn't a hot path... it's fine.
        String vl = String.valueOf(group.getCachedChecksViolations(check.getClass()));

        original = original.replace("[alert]", alertString);
        original = original.replace("[proxy]", alertString);
        original = original.replace("%check_name%", check.getCheckName());
        original = original.replace("%vl%", vl);
        original = original.replace("%verbose%", verbose);
        original = MessageUtil.format(original);
        original = GrimAPI.INSTANCE.getExternalAPI().replaceVariables(player, original, true);

        return original;
    }

    public boolean handleAlert(GrimPlayer player, String verbose, Check check) {
        String alertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("alerts-format", "%prefix% &f%player% &bfailed &f%check_name% &f(x&c%vl%&f) &7%verbose%");
        boolean testMode = GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("test-mode", false);
        boolean sentDebug = false;
        // Check commands
        for (PunishGroup group : groups) {
            if (group.getCachedPunishGroup().getChecks().contains(check.getClass())) {
                int violationCount = group.getViolations().size();
                for (ParsedCommand command : group.getCachedPunishGroup().getCommands()) {
                    String cmd = replaceAlertPlaceholders(player, command.getCommand(), group, check, alertString, verbose);

                    // Verbose that prints all flags
                    if (GrimAPI.INSTANCE.getAlertManager().getEnabledVerbose().size() > 0 && command.command.equals("[alert]")) {
                        sentDebug = true;
                        for (Player bukkitPlayer : GrimAPI.INSTANCE.getAlertManager().getEnabledVerbose()) {
                            bukkitPlayer.sendMessage(cmd);
                        }
                        if (GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("verbose.print-to-console", false)) {
                            LogUtil.console(cmd); // Print verbose to console
                        }
                    }
                    if (violationCount >= command.getThreshold()) {
                        // 0 means execute once
                        // Any other number means execute every X interval
                        boolean inInterval = command.getInterval() == 0 ? (group.getExecuteCount(command) == 0) : (violationCount % command.getInterval() == 0);
                        if (inInterval) {
                            CommandExecuteEvent executeEvent = new CommandExecuteEvent(player, check, cmd);
                            Bukkit.getPluginManager().callEvent(executeEvent);
                            if (executeEvent.isCancelled()) continue;

                            if (command.command.equals("[webhook]")) {
                                String vl = String.valueOf(group.getCachedChecksViolations(check.getClass()));
                                GrimAPI.INSTANCE.getDiscordManager().sendAlert(player, verbose, check.getCheckName(), vl);
                                continue;
                            }

                            if (command.command.equals("[proxy]")) {
                                String proxyAlertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("alerts-format-proxy", "%prefix% &f[&cproxy&f] &f%player% &bfailed &f%check_name% &f(x&c%vl%&f) &7%verbose%");
                                proxyAlertString = replaceAlertPlaceholders(player, command.getCommand(), group, check, proxyAlertString, verbose);
                                ProxyAlertMessenger.sendPluginMessage(proxyAlertString);
                                continue;
                            }

                            if (command.command.equals("[alert]")) {
                                sentDebug = true;
                                if (testMode) { // secret test mode
                                    player.user.sendMessage(cmd);
                                    continue;
                                }
                                cmd = "grimac sendalert " + cmd; // Not test mode, we can add the command prefix
                            }

                            String finalCmd = cmd;
                            FoliaCompatUtil.runTask(GrimAPI.INSTANCE.getPlugin(), (dummy) -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd));
                        }
                        group.incrementExecute(command);
                    }
                }
            }
        }

        return sentDebug;
    }

    public void handleViolation(Check check) {
        for (PunishGroup group : groups) {
            if (group.getCachedPunishGroup().getChecks().contains(check.getClass())) {
                long currentTime = System.currentTimeMillis();
                group.getViolations().put(currentTime, check);
                group.incrementCachedChecksViolations(check.getClass());
                this.violated = true;
            }
        }
    }
}

@Builder
class CachedPunishGroup {
    @Getter
    private List<Class<? extends AbstractCheck>> checks;
    @Getter
    private List<ParsedCommand> commands;
    @Getter
    private int removeViolationsAfter;
}

class PunishGroup {
    @Getter
    private final CachedPunishGroup cachedPunishGroup;
    @Getter
    private final Map<Long, Check> violations;
    @Getter
    private final Map<ParsedCommand, Integer> commandsExecutes;
    @Getter
    private final Map<Class<? extends AbstractCheck>, Integer> cachedChecks;

    public PunishGroup(CachedPunishGroup cachedPunishGroup) {
        this.cachedPunishGroup = cachedPunishGroup;
        this.violations = new HashMap<>();
        this.commandsExecutes = new HashMap<>(cachedPunishGroup.getCommands().size());
        cachedPunishGroup.getCommands().forEach(parsedCommand -> this.commandsExecutes.put(parsedCommand, 0));
        this.cachedChecks = new HashMap<>(cachedPunishGroup.getChecks().size());
    }

    public void incrementExecute(ParsedCommand parsedCommand) {
        commandsExecutes.put(parsedCommand, commandsExecutes.get(parsedCommand) + 1);
    }

    public int getExecuteCount(ParsedCommand parsedCommand) {
        return commandsExecutes.get(parsedCommand);
    }

    public int getCachedChecksViolations(Class<? extends AbstractCheck> aClass) {
        return getCachedChecks().getOrDefault(aClass, 0);
    }

    public void cleanup() {
        // Remove violations older than the defined time in the config
        long currentTime = System.currentTimeMillis();
        getViolations().entrySet().removeIf(time -> {
            if (currentTime - time.getKey() > getCachedPunishGroup().getRemoveViolationsAfter()) {
                decrementCachedChecksViolations(time.getValue().getClass());
                return true;
            }
            return false;
        });
    }
    public void incrementCachedChecksViolations(Class<? extends AbstractCheck> aClass) {
        getCachedChecks().put(aClass, getCachedChecksViolations(aClass) + 1);
    }

    public void decrementCachedChecksViolations(Class<? extends AbstractCheck> aClass) {
        getCachedChecks().put(aClass, getCachedChecksViolations(aClass) - 1);
    }
}

class ParsedCommand {
    @Getter
    int threshold;
    @Getter
    int interval;
    @Getter
    String command;

    public ParsedCommand(int threshold, int interval, String command) {
        this.threshold = threshold;
        this.interval = interval;
        this.command = command;
    }
}
