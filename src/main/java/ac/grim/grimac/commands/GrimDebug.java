package ac.grim.grimac.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@CommandAlias("grim|grimac")
public class GrimDebug extends BaseCommand {
    @Subcommand("debug")
    @CommandPermission("grim.debug")
    @CommandCompletion("@players")
    public void onDebug(Player player, @Optional OnlinePlayer target) {
        GrimPlayer grimPlayer = parseTarget(player, target);
        if (grimPlayer == null) return;

        grimPlayer.checkManager.getDebugHandler().toggleListener(player);
    }

    private GrimPlayer parseTarget(Player player, OnlinePlayer target) {
        Player targetPlayer = target == null ? player : target.getPlayer();

        GrimPlayer grimPlayer = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(targetPlayer);
        if (grimPlayer == null) player.sendMessage(ChatColor.RED + "This player is exempt from all checks!");

        return grimPlayer;
    }

    @Subcommand("consoledebug")
    @CommandPermission("grim.consoledebug")
    @CommandCompletion("@players")
    public void onConsoleDebug(Player player, @Optional OnlinePlayer target) {
        GrimPlayer grimPlayer = parseTarget(player, target);
        if (grimPlayer == null) return;

        boolean isOutput = grimPlayer.checkManager.getDebugHandler().toggleConsoleOutput();

        player.sendMessage("Console output for " + grimPlayer.bukkitPlayer.getName() + " is now " + isOutput);
    }
}