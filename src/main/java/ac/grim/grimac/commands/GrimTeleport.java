package ac.grim.grimac.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("grim|grimac")
public class GrimTeleport extends BaseCommand {
    @Subcommand("teleport")
    @CommandPermission("grim.teleport")
    @CommandCompletion("@players")
    public void onTeleport(CommandSender sender, OnlinePlayer target) {
        Player player = null;
        if (sender instanceof Player) player = (Player) sender;

        assert player != null;
        player.teleport(target.getPlayer());
    }
}
