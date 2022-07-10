package ac.grim.grimac.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import org.bukkit.command.CommandSender;

@CommandAlias("gl")
public class GrimLogShortcut extends BaseCommand {
    @Default
    @CommandPermission("grim.log")
    public void grimLog(CommandSender sender, int flagId) { // TODO: There has to be a better way to make an alias
        new GrimLog().onLog(sender, flagId);
    }
}
