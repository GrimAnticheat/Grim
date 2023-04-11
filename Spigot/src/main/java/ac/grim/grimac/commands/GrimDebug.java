package ac.grim.grimac.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

@CommandAlias("grim|grimac")
public class GrimDebug extends BaseCommand {
    @Subcommand("debug")
    @CommandPermission("grim.debug")
    @CommandCompletion("@players")
    public void onDebug(CommandSender sender, @Optional OnlinePlayer target) {
        Player player = null;
        if (sender instanceof Player) player = (Player) sender;

        GrimPlayer grimPlayer = parseTarget(sender, player, target);
        if (grimPlayer == null) return;

        if (sender instanceof ConsoleCommandSender) { // Just debug to console to reduce complexity...
            grimPlayer.checkManager.getDebugHandler().toggleConsoleOutput();
        } else { // This sender is a player
            grimPlayer.checkManager.getDebugHandler().toggleListener(player);
        }
    }

    private GrimPlayer parseTarget(CommandSender sender, Player player, OnlinePlayer target) {
        Player targetPlayer = target == null ? player : target.getPlayer();
        if (player == null && target == null) {
            sender.sendMessage(ChatColor.RED + "You must specify a target as the console!");
            return null;
        }

        GrimPlayer grimPlayer = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(targetPlayer);
        if (grimPlayer == null) {
            User user = PacketEvents.getAPI().getPlayerManager().getUser(targetPlayer);
            sender.sendMessage(ChatColor.RED + "This player is exempt from all checks!");

            if (user == null) {
                sender.sendMessage(ChatColor.RED + "Unknown PacketEvents user");
            } else {
                boolean isExempt = GrimAPI.INSTANCE.getPlayerDataManager().shouldCheck(user);
                if (!isExempt) {
                    sender.sendMessage(ChatColor.RED + "User connection state: " + user.getConnectionState());
                }
            }
        }

        return grimPlayer;
    }

    @Subcommand("consoledebug")
    @CommandPermission("grim.consoledebug")
    @CommandCompletion("@players")
    public void onConsoleDebug(CommandSender sender, @Optional OnlinePlayer target) {
        Player player = null;
        if (sender instanceof Player) player = (Player) sender;

        GrimPlayer grimPlayer = parseTarget(sender, player, target);
        if (grimPlayer == null) return;

        boolean isOutput = grimPlayer.checkManager.getDebugHandler().toggleConsoleOutput();

        sender.sendMessage("Console output for " + (grimPlayer.bukkitPlayer == null ? grimPlayer.user.getProfile().getName() : grimPlayer.bukkitPlayer.getName()) + " is now " + isOutput);
    }
}