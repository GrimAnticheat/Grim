package ac.grim.legacyac.command;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Locale;

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
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Usage: /glac <alerts|reload|info|profile|debug>");
        return true;
    }
}
