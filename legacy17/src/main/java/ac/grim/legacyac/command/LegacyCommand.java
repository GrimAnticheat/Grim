package ac.grim.legacyac.command;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.debug.DetectionEvidence;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class LegacyCommand implements CommandExecutor {
    private static final String[] CHECKS = new String[] {"Speed", "Fly", "Phase", "Reach", "AutoClicker", "NoFall", "KillAura", "Timer", "Velocity", "Jesus", "FastPlace", "FastBreak", "FastUse", "InventoryMove", "Prediction", "NoSlow"};
    private final LegacyAntiCheatPlugin plugin;

    public LegacyCommand(LegacyAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("grimlegacy.command")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (args.length == 0 || "info".equalsIgnoreCase(args[0])) {
            sender.sendMessage(ChatColor.GOLD + "GrimLegacyAC " + ChatColor.GRAY + "- 1.7.10 focused anticheat.");
            sender.sendMessage(ChatColor.GRAY + "Checks: " + plugin.checks().getCheckCount());
            return true;
        }
        if ("reload".equalsIgnoreCase(args[0])) {
            plugin.reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "GrimLegacyAC config reloaded.");
            return true;
        }
        if ("alerts".equalsIgnoreCase(args[0])) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Player only.");
                return true;
            }
            boolean enabled = plugin.alerts().toggle((Player) sender);
            sender.sendMessage((enabled ? ChatColor.GREEN : ChatColor.YELLOW) + "Alerts " + (enabled ? "enabled" : "disabled") + ".");
            return true;
        }

        if ("debug".equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /glac debug <player>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
            PlayerData data = plugin.getPlayerData(target);
            data.setDebugEnabled(!data.isDebugEnabled());
            sender.sendMessage((data.isDebugEnabled() ? ChatColor.GREEN : ChatColor.YELLOW)
                + "Debug for " + target.getName() + " " + (data.isDebugEnabled() ? "enabled" : "disabled") + ".");
            return true;
        }

        if ("profile".equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /glac profile <player>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
            PlayerData data = plugin.getPlayerData(target);
            sender.sendMessage(ChatColor.GOLD + "[GLAC] " + ChatColor.GRAY + target.getName() + " VL profile");
            for (String check : CHECKS) {
                sender.sendMessage(ChatColor.DARK_GRAY + " - " + ChatColor.YELLOW + check + ChatColor.GRAY + ": "
                    + String.format(Locale.ROOT, "%.2f", data.getViolation(check)));
            }
            sender.sendMessage(ChatColor.DARK_GRAY + " - " + ChatColor.AQUA + "RTT" + ChatColor.GRAY + ": "
                + String.format(Locale.ROOT, "%.2fms", data.getLastTransactionRttNanos() / 1000000.0D));
            sender.sendMessage(ChatColor.DARK_GRAY + " - " + ChatColor.AQUA + "lastTransTime" + ChatColor.GRAY + ": "
                + data.getLastTransTime());
            sender.sendMessage(ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "P95 offset" + ChatColor.GRAY + ": "
                + String.format(Locale.ROOT, "%.4f", data.getDetectionOffsetP95()));
            sender.sendMessage(ChatColor.DARK_GRAY + " - " + ChatColor.LIGHT_PURPLE + "trigger chain" + ChatColor.GRAY + ": "
                + data.getRecentTriggerChain(8));
            PlayerData.VelocitySample velocitySample = data.getCurrentVelocitySample();
            if (velocitySample != null) {
                sender.sendMessage(ChatColor.DARK_GRAY + " - " + ChatColor.BLUE + "VelocityTX" + ChatColor.GRAY + ": "
                    + "pre=" + velocitySample.getPreTxId()
                    + ", post=" + velocitySample.getPostTxId()
                    + ", ticks=" + velocitySample.getTicksObserved()
                    + ", minOffset=" + String.format(Locale.ROOT, "%.4f", velocitySample.getMinOffset())
                    + ", flags=" + velocitySample.getStateFlags());
            }
            sender.sendMessage(ChatColor.DARK_GRAY + " - " + ChatColor.BLUE + "VelocitySamples" + ChatColor.GRAY + ": "
                + data.getVelocitySampleQueueSize());
            return true;
        }

        if ("dump".equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /glac dump <player>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
            PlayerData data = plugin.getPlayerData(target);
            sender.sendMessage(ChatColor.GRAY + toJson(target, data));
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Usage: /glac <alerts|reload|info|profile|debug|dump>");
        return true;
    }

    private String toJson(Player target, PlayerData data) {
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        appendField(builder, "player", target.getName());
        builder.append(',');
        appendField(builder, "uuid", target.getUniqueId().toString());
        builder.append(',');
        appendField(builder, "p95Offset", String.format(Locale.ROOT, "%.6f", data.getDetectionOffsetP95()), false);
        builder.append(',');
        appendField(builder, "triggerChain", data.getRecentTriggerChain(12));
        builder.append(',');
        appendField(builder, "scenario", data.getScenarioTag());
        builder.append(',');
        PlayerData.VelocitySample velocitySample = data.getCurrentVelocitySample();
        builder.append("\"velocity\":{");
        appendField(builder, "queueSize", String.valueOf(data.getVelocitySampleQueueSize()), false);
        if (velocitySample != null) {
            builder.append(',');
            appendField(builder, "preTxId", String.valueOf(velocitySample.getPreTxId()), false);
            builder.append(',');
            appendField(builder, "postTxId", String.valueOf(velocitySample.getPostTxId()), false);
            builder.append(',');
            appendField(builder, "ticksObserved", String.valueOf(velocitySample.getTicksObserved()), false);
            builder.append(',');
            appendField(builder, "minOffset", String.format(Locale.ROOT, "%.6f", velocitySample.getMinOffset()), false);
            builder.append(',');
            appendField(builder, "stateFlags", String.valueOf(velocitySample.getStateFlags()), false);
        }
        builder.append('}');
        builder.append(',');
        builder.append("\"evidence\":[");
        List<DetectionEvidence> evidenceList = data.getDetectionEvidenceSnapshot();
        for (int i = 0; i < evidenceList.size(); i++) {
            DetectionEvidence evidence = evidenceList.get(i);
            if (i > 0) {
                builder.append(',');
            }
            builder.append('{');
            appendField(builder, "ts", String.valueOf(evidence.getTimestampMillis()), false);
            builder.append(',');
            appendField(builder, "check", evidence.getCheck());
            builder.append(',');
            appendField(builder, "offset", String.format(Locale.ROOT, "%.6f", evidence.getOffset()), false);
            builder.append(',');
            appendField(builder, "buffer", String.format(Locale.ROOT, "%.6f", evidence.getBuffer()), false);
            builder.append(',');
            appendField(builder, "vl", String.format(Locale.ROOT, "%.6f", evidence.getVl()), false);
            builder.append(',');
            appendField(builder, "rtt", String.format(Locale.ROOT, "%.3f", evidence.getRtt()), false);
            builder.append(',');
            appendField(builder, "source", evidence.getSource());
            builder.append(',');
            appendField(builder, "tick", String.valueOf(evidence.getTick()), false);
            builder.append('}');
        }
        builder.append("]}");
        return builder.toString();
    }

    private void appendField(StringBuilder builder, String key, String value) {
        appendField(builder, key, value, true);
    }

    private void appendField(StringBuilder builder, String key, String value, boolean quoted) {
        builder.append('"').append(escapeJson(key)).append('"').append(':');
        if (quoted) {
            builder.append('"').append(escapeJson(value)).append('"');
        } else {
            builder.append(value);
        }
    }

    private String escapeJson(String value) {
        String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"");
        return escaped.replace("\n", "\\n").replace("\r", "\\r");
    }
}
