package ac.grim.grimac.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.BukkitCommandCompletionContext;
import co.aikar.commands.CommandCompletions.CommandCompletionHandler;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@CommandAlias("grim|grimac")
public class GrimStopSpectating extends BaseCommand {
    public static final CommandCompletionHandler<BukkitCommandCompletionContext> completionHandler = context -> context.getSender().hasPermission("grim.spectate.stophere") ? List.of("here") : List.of();

    @Subcommand("stopspectating")
    @CommandPermission("grim.spectate")
    @CommandCompletion("@stopspectating")
    public void onStopSpectate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) return;
        String string = args.length > 0 ? args[0] : null;
        if (GrimAPI.INSTANCE.getSpectateManager().isSpectating(player.getUniqueId())) {
            boolean teleportBack = string == null || !string.equalsIgnoreCase("here") || !sender.hasPermission("grim.spectate.stophere");
            GrimAPI.INSTANCE.getSpectateManager().disable(GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(player), teleportBack);
        } else {
            String message = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("cannot-spectate-return", "%prefix% &cYou can only do this after spectating a player.");
            message = MessageUtil.replacePlaceholders(sender, message);
            MessageUtil.sendMessage(sender, MessageUtil.miniMessage(message));
        }
    }
}
