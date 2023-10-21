package ac.grim.grimac.manager;

import ac.grim.grimac.AbstractCheck;
import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.events.CommandExecuteEvent;
import ac.grim.grimac.events.packets.ProxyAlertMessenger;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import com.github.retrooper.packetevents.util.adventure.AdventureSerializer;
import github.scarsz.configuralize.DynamicConfig;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

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

                groups.add(new PunishGroup(checksList, parsed, removeViolationsAfter));
            }
        } catch (Exception e) {
            LogUtil.error("Error while loading punishments.yml! This is likely your fault!");
            e.printStackTrace();
        }
    }

    private String replaceAlertPlaceholders(String original, PunishGroup group, Check check, String alertString, String verbose) {
        // Streams are slow but this isn't a hot path... it's fine.
        String vl = group.violations.values().stream().filter((e) -> e == check).count() + "";

        original = original.replace("[alert]", alertString);
        original = original.replace("[proxy]", alertString);
        original = original.replace("%check_name%", check.getCheckName());
        original = original.replace("%vl%", vl);
        original = original.replace("%verbose%", verbose);
        original = MessageUtil.format(original);
        original = GrimAPI.INSTANCE.getExternalAPI().replaceVariables(player, original, true);

        return original;
    }

    public String replaceHoverPlaceholders(Check check, List<String> hoverString, String verbose, String... info) {
        String line = String.join("\n", hoverString);
        String details = String.join("\n", info);
        line = line.replace("%description%", check.getDescription());
        line = line.replace("%check_name%", check.getCheckName());
        if (details.isEmpty()) {
            line = line.replace("%info%", "None provided");
        } else {
            line = line.replace("%info%", details);
        }
        line = line.replace("%verbose%", verbose);
        line = GrimAPI.INSTANCE.getExternalAPI().replaceVariables(player, line, true);
        return line;
    }

    public boolean handleAlert(GrimPlayer player, String verbose, Check check, String... info) {
        String alertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("alerts-format", "%prefix% &f%player% &bfailed &f%check_name% &f(x&c%vl%&f)");
        String clickAction = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("click-action", "grim spectate %player%").replaceAll("%player%", player.getName());
        List<String> hoverList = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringListElse("hover-format", Arrays.asList("%prefix%", "§f  Ping §8» §b%ping%", "§f  Version §8» §b%brand% %version%", "§f  Verbose §8» §b%verbose%", "", "&bClick to Execute the Command!"));
        //String verboseString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("verbose-format", "%prefix% &f%player% &bfailed &f%check_name% &f(x&c%vl%&f) ");
        boolean testMode = GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("test-mode", false);
        boolean sentDebug = false;

        if (!clickAction.startsWith("/")) clickAction = "/" + clickAction;

        // Check commands
        for (PunishGroup group : groups) {
            if (group.getChecks().contains(check)) {
                int violationCount = group.getViolations().size();
                for (ParsedCommand command : group.getCommands()) {
                    String alert = replaceAlertPlaceholders(command.getCommand(), group, check, alertString, verbose);
                    //String console = replaceAlertPlaceholders(command.getCommand(), group, check, verboseString, verbose);
                    String hover = replaceHoverPlaceholders(check, hoverList, verbose, info);

                    // Verbose that prints all flags
                    if (GrimAPI.INSTANCE.getAlertManager().getEnabledVerbose().size() > 0 && command.command.equals("[alert]")) {
                        sentDebug = true;
                        for (Player bukkitPlayer : GrimAPI.INSTANCE.getAlertManager().getEnabledVerbose()) {
                            TextComponent message = new TextComponent(TextComponent.fromLegacyText(alert));
                            message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, clickAction)); //Old: "/grim spectate "+player.getName()
                            message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hover).create()));

                            bukkitPlayer.spigot().sendMessage(message);
                        }
                        if (GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("verbose.print-to-console", false)) {
                            LogUtil.console(alert+" §7"+verbose); // Print verbose to console
                        }
                    }

                    if (violationCount >= command.getThreshold()) {
                        // 0 means execute once
                        // Any other number means execute every X interval
                        boolean inInterval = command.getInterval() == 0 ? (command.executeCount == 0) : (violationCount % command.getInterval() == 0);
                        if (inInterval) {
                            CommandExecuteEvent executeEvent = new CommandExecuteEvent(player, check, alert);
                            Bukkit.getPluginManager().callEvent(executeEvent);
                            if (executeEvent.isCancelled()) continue;

                            if (command.command.equals("[webhook]")) {
                                String vl = group.violations.values().stream().filter((e) -> e == check).count() + "";
                                GrimAPI.INSTANCE.getDiscordManager().sendAlert(player, verbose, check.getCheckName(), vl);
                            } else if (command.command.equals("[proxy]")) {
                                String proxyAlertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("alerts-format-proxy", "%prefix% &f[&cproxy&f] &f%player% &bfailed &f%check_name% &f(x&c%vl%&f) &7%verbose%");
                                proxyAlertString = replaceAlertPlaceholders(command.getCommand(), group, check, proxyAlertString, verbose);
                                ProxyAlertMessenger.sendPluginMessage(proxyAlertString);
                            } else {
                                //Made by FrancyPro for GrimAC
                                if (command.command.equals("[alert]")) {
                                    sentDebug = true;
                                    for (Player bukkitPlayer : GrimAPI.INSTANCE.getAlertManager().getEnabledAlerts()) {
                                        TextComponent message = new TextComponent(alert);
                                        message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/"+clickAction)); //Old: "/grim spectate "+player.getName()
                                        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hover).create()));

                                        bukkitPlayer.spigot().sendMessage(message);
                                    }

                                    if (GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("alerts.print-to-console", true)) {
                                        LogUtil.console(alert+" §7"+verbose); // Print alert to console
                                    }
                                }
                            }
                        }

                        command.setExecuteCount(command.getExecuteCount() + 1);
                    }
                }
            }
        }
        return sentDebug;
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
    List<AbstractCheck> checks;
    @Getter
    List<ParsedCommand> commands;
    @Getter
    HashMap<Long, Check> violations = new HashMap<>();
    @Getter
    int removeViolationsAfter;

    public PunishGroup(List<AbstractCheck> checks, List<ParsedCommand> commands, int removeViolationsAfter) {
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
