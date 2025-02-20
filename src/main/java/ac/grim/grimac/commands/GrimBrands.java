package ac.grim.grimac.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import org.bukkit.entity.Player;

@CommandAlias("grim|grimac")
public class GrimBrands extends BaseCommand {
    @Subcommand("brands")
    @CommandPermission("grim.brand")
    public void onBrands(Player player) {
        GrimPlayer grimPlayer = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(player);
        GrimAPI.INSTANCE.getAlertManager().toggleBrands(grimPlayer);
    }
}
