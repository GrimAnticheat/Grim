package ac.grim.grimac.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import org.bukkit.command.CommandSender;

@CommandAlias("grim|grimac")
public class GrimPerf extends BaseCommand {
    @Subcommand("perf|performance")
    @CommandPermission("grim.performance")
    public void onPerformance(CommandSender sender) {
        /*double nano = MovementCheckRunner.executor.getLongComputeTime() * 20 * GrimAPI.INSTANCE.getPlayerDataManager().size();
        // Convert this into seconds
        double seconds = nano / 1e9;

        sender.sendMessage("Nanoseconds per prediction: " + MovementCheckRunner.executor.getComputeTime());
        sender.sendMessage("Estimated load (threads): " + seconds);
        sender.sendMessage("Prediction threads: " + MovementCheckRunner.executor.getPoolSize());
        sender.sendMessage("Players online: " + GrimAPI.INSTANCE.getPlayerDataManager().size());*/
        sender.sendMessage("This command is currently broken.");
    }
}
