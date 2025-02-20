package ac.grim.grimac.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GameMode;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("grim|grimac")
public class GrimSpectate extends BaseCommand {
    @Subcommand("spectate")
    @CommandPermission("grim.spectate")
    @CommandCompletion("@players")
    public void onSpectate(CommandSender sender, @Optional OnlinePlayer t) {
        if (!(sender instanceof Player p)) return;
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(p);
        GrimPlayer target = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(t.getPlayer());

        if (target != null && target.getUniqueId().equals(player.getUniqueId())) {
            String message = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("cannot-run-on-self", "%prefix% &cYou cannot use this command on yourself!");
            message = MessageUtil.replacePlaceholders(target, message);
            MessageUtil.sendMessage(player, MessageUtil.miniMessage(message));
            return;
        }

        if (target == null || (target.platformPlayer != null && target.platformPlayer.isExternalPlayer())) {
            String message = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("player-not-this-server", "%prefix% &cThis player isn't on this server!");
            message = MessageUtil.replacePlaceholders(target, message);
            MessageUtil.sendMessage(player, MessageUtil.miniMessage(message));
            return;
        }

        // hide player from tab list
        if (GrimAPI.INSTANCE.getSpectateManager().enable(player)) {
            String message = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("spectate-return", "<click:run_command:/grim stopspectating><hover:show_text:\"/grim stopspectating\">\n%prefix% &fClick here to return to previous location\n</hover></click>");
            message = MessageUtil.replacePlaceholders(target, message);
            player.user.sendMessage(MessageUtil.miniMessage(message));
        }

        player.setGameMode(GameMode.SPECTATOR);
        player.platformPlayer.teleportAsync(target.getLocation());
    }
}
