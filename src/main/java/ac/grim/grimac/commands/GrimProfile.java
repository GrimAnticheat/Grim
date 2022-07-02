package ac.grim.grimac.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.impl.aim.processor.AimProcessor;
import ac.grim.grimac.checks.impl.misc.ClientBrand;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.math.GrimMath;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import com.github.puregero.multilib.MultiLib;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("grim|grimac")
public class GrimProfile extends BaseCommand {
    @Subcommand("profile")
    @CommandPermission("grim.profile")
    @CommandCompletion("@players")
    public void onConsoleDebug(CommandSender sender, OnlinePlayer target) {
        Player player = null;
        if (sender instanceof Player) player = (Player) sender;

        if (MultiLib.isExternalPlayer(target.getPlayer())) {
            String alertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("player-not-this-server", "%prefix% &cPlayer isn't on this server!");
            sender.sendMessage(MessageUtil.format(alertString));
            return;
        }

        GrimPlayer grimPlayer = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(target.getPlayer());
        if (grimPlayer == null) {
            String message = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("player-not-found", "%prefix% &cPlayer is exempt or offline!");
            sender.sendMessage(MessageUtil.format(message));
            return;
        }

        ClientBrand brand = grimPlayer.checkManager.getPacketCheck(ClientBrand.class);
        AimProcessor aimProcessor = grimPlayer.checkManager.getRotationCheck(AimProcessor.class);


        String hSens = ((int) Math.round(aimProcessor.sensitivityX * 200)) + "";
        String vSens = ((int) Math.round(aimProcessor.sensitivityY * 200)) + "";
        String fastMath = !grimPlayer.trigHandler.isVanillaMath() + "";
        String formattedPing = "" + GrimMath.floor(grimPlayer.getTransactionPing() / 1e6);

        for (String message : GrimAPI.INSTANCE.getConfigManager().getConfig().getStringList("profile")) {
            message = MessageUtil.format(message);
            message = message.replace("%ping%", formattedPing);
            message = message.replace("%player%", target.getPlayer().getName());
            message = message.replace("%version%", grimPlayer.getClientVersion().getReleaseName());
            message = message.replace("%brand%", brand.getBrand());
            message = message.replace("%h_sensitivity%", hSens);
            message = message.replace("%v_sensitivity%", vSens);
            message = message.replace("%fast_math%", fastMath);

            sender.sendMessage(message);
        }
    }
}
