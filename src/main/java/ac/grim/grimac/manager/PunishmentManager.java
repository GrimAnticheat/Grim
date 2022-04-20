package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.events.CommandExecuteEvent;
import github.scarsz.configuralize.DynamicConfig;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;

import java.util.*;

public class PunishmentManager {
    GrimPlayer player;
    List<PunishGroup> groups = new ArrayList<>();

    public PunishmentManager(GrimPlayer player) {
        this.player = player;
        reload();
    }

    public void reload() {
        DynamicConfig config = GrimAPI.INSTANCE.getConfigManager().getConfig();
        List<String> punish = config.getStringListElse("Punishments", new ArrayList<>());

        try {
            for (Object s : punish) {
                LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) s;

                List<String> checks = (List<String>) map.getOrDefault("checks", new ArrayList<>());
                List<String> commands = (List<String>) map.getOrDefault("commands", new ArrayList<>());
                int removeViolationsAfter = (int) map.getOrDefault("removeViolationsAfter", 300);

                List<ParsedCommand> parsed = new ArrayList<>();
                List<Check> checksList = new ArrayList<>();

                for (String command : checks) {
                    command = command.toLowerCase(Locale.ROOT);
                    for (Check check : player.checkManager.allChecks.values()) { // o(n) * o(n)?
                        if (check.getCheckName() != null && check.getCheckName().toLowerCase(Locale.ROOT).contains(command)) {
                            checksList.add(check);
                        }
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

                groups.add(new PunishGroup(checksList, parsed, removeViolationsAfter));
            }
        } catch (Exception e) {
            LogUtil.error("Error while loading punishments.yml! This is likely your fault!");
            e.printStackTrace();
        }
    }

    public void handleAlert(GrimPlayer player, String verbose, Check check) {
        String alertString = "grim sendalert " + GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("alerts-format", "%prefix% &f%player% &bfailed &f%check_name% &f(x&c%vl%&f) &7%verbose%");
        boolean testMode = GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("test-mode", false);

        // Check commands
        for (PunishGroup group : groups) {
            if (group.getChecks().contains(check)) {
                int violationCount = group.getViolations().size();

                for (ParsedCommand command : group.getCommands()) {
                    if (violationCount >= command.getThreshold()) {
                        boolean inInterval = command.getInterval() == 0 || violationCount % command.getInterval() == 0;

                        if (inInterval) {
                            String cmd = command.getCommand();

                            // Streams are slow but this isn't a hot path... it's fine.
                            String vl = group.violations.values().stream().filter((e) -> e == check).count() + "";

                            cmd = cmd.replace("[alert]", alertString);
                            cmd = cmd.replace("%check_name%", check.getCheckName());
                            cmd = cmd.replace("%vl%", vl);
                            cmd = cmd.replace("%verbose%", verbose);

                            CommandExecuteEvent executeEvent = new CommandExecuteEvent(check, cmd);
                            Bukkit.getPluginManager().callEvent(executeEvent);
                            if (executeEvent.isCancelled()) continue;

                            if (cmd.equals("[webhook]")) {
                                GrimAPI.INSTANCE.getDiscordManager().sendAlert(player, verbose, check.getCheckName(), vl);
                                continue;
                            }

                            if (player.bukkitPlayer != null) {
                                cmd = cmd.replace("%player%", player.bukkitPlayer.getName());
                            }

                            if (testMode && cmd.contains("grim sendalert")) { // secret test mode
                                cmd = MessageUtil.format(cmd);
                                player.user.sendMessage(cmd.replace("grim sendalert ", ""));
                                continue;
                            }

                            String finalCmd = cmd;
                            Bukkit.getScheduler().runTask(GrimAPI.INSTANCE.getPlugin(), () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd));
                        }

                        command.setExecuteCount(command.getExecuteCount() + 1);
                    }
                }
            }
        }
    }

    public void handleViolation(Check check) {
        for (PunishGroup group : groups) {
            if (group.getChecks().contains(check)) {
                long currentTime = System.currentTimeMillis();

                group.violations.put(currentTime, check);
                // Remove violations older than the defined time in the config
                group.violations.entrySet().removeIf(time -> currentTime - time.getKey() > group.removeViolationsAfter);
            }
        }
    }
}

class PunishGroup {
    @Getter
    List<Check> checks;
    @Getter
    List<ParsedCommand> commands;
    @Getter
    HashMap<Long, Check> violations = new HashMap<>();
    @Getter
    int removeViolationsAfter;

    public PunishGroup(List<Check> checks, List<ParsedCommand> commands, int removeViolationsAfter) {
        this.checks = checks;
        this.commands = commands;
        this.removeViolationsAfter = removeViolationsAfter * 1000;
    }
}

class ParsedCommand {
    @Getter
    int threshold;
    @Getter
    int interval;
    @Getter
    @Setter
    int executeCount;
    @Getter
    String command;

    public ParsedCommand(int threshold, int interval, String command) {
        this.threshold = threshold;
        this.interval = interval;
        this.command = command;
    }
}