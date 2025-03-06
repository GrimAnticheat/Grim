package ac.grim.grimac.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.anticheat.MultiLibUtil;
import ac.grim.grimac.utils.reflection.PaperUtils;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("grim|grimac")
public class GrimSpectate extends BaseCommand {
    @Subcommand("spectate")
    @CommandPermission("grim.spectate")
    @CommandCompletion("@players")
    public void onSpectate(CommandSender sender, @Optional Player target) {
        if (!(sender instanceof Player player)) return;

        if (target != null && target.getUniqueId().equals(player.getUniqueId())) {
            String message = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("cannot-run-on-self", "%prefix% &cYou cannot use this command on yourself!");
            message = MessageUtil.replacePlaceholders(target, message);
            MessageUtil.sendMessage(player, MessageUtil.miniMessage(message));
            return;
        }

        if (target == null || (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_18) && MultiLibUtil.isExternalPlayer(target.getPlayer()))) {
            String message = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("player-not-this-server", "%prefix% &cThis player isn't on this server!");
            message = MessageUtil.replacePlaceholders(target, message);
            MessageUtil.sendMessage(player, MessageUtil.miniMessage(message));
            return;
        }

        // hide player from tab list
        if (GrimAPI.INSTANCE.getSpectateManager().enable(player)) {
            GrimPlayer grimPlayer = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(player);
            if (grimPlayer != null) {
                String message = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("spectate-return", "<click:run_command:/grim stopspectating><hover:show_text:\"/grim stopspectating\">\n%prefix% &fClick here to return to previous location\n</hover></click>");
                message = MessageUtil.replacePlaceholders(target, message);
                grimPlayer.user.sendMessage(MessageUtil.miniMessage(message));
            }
        }

        player.setGameMode(GameMode.SPECTATOR);
        PaperUtils.teleportAsync(player, target.getLocation());
    }
}
