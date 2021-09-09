package ac.grim.grimac.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.predictionengine.MovementCheckRunner;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import org.bukkit.entity.Player;

@CommandAlias("grim|grimac")
public class GrimPerf extends BaseCommand {
    @Subcommand("perf|performance")
    @CommandPermission("grim.performance")
    public void onPerformance(Player player) {
        double nano = MovementCheckRunner.executor.getLongComputeTime() * 20 * GrimAPI.INSTANCE.getPlayerDataManager().size();
        // Convert this into seconds
        double seconds = nano / 1e9;

        player.sendMessage("Nanoseconds per prediction: " + MovementCheckRunner.executor.getComputeTime());
        player.sendMessage("Estimated load (threads): " + seconds);
        player.sendMessage("Prediction threads: " + MovementCheckRunner.executor.getPoolSize());
        player.sendMessage("Players online: " + GrimAPI.INSTANCE.getPlayerDataManager().size());
    }
}
