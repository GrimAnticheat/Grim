package ac.grim.bukkit.utils.scheduler.bukkit;

import ac.grim.bukkit.GrimACBukkitLoaderPlugin;
import ac.grim.grimac.api.GrimPlugin;
import ac.grim.grimac.platform.api.scheduler.GlobalRegionScheduler;
import ac.grim.grimac.platform.api.scheduler.TaskHandle;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

public class BukkitGlobalRegionScheduler implements GlobalRegionScheduler {

    private final BukkitScheduler bukkitScheduler = Bukkit.getScheduler();

    @Override
    public void execute(@NotNull GrimPlugin plugin, @NotNull Runnable run) {
        bukkitScheduler.runTask(GrimACBukkitLoaderPlugin.PLUGIN, run);
    }

    @Override
    public TaskHandle run(@NotNull GrimPlugin plugin, @NotNull Runnable task) {
        return new BukkitTaskHandle(bukkitScheduler.runTask(GrimACBukkitLoaderPlugin.PLUGIN, task));
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimPlugin plugin, @NotNull Runnable task, long delay) {
        return new BukkitTaskHandle(bukkitScheduler.runTaskLater(GrimACBukkitLoaderPlugin.PLUGIN, task, delay));
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return new BukkitTaskHandle(bukkitScheduler.runTaskTimer(GrimACBukkitLoaderPlugin.PLUGIN, task, initialDelayTicks, periodTicks));
    }

    @Override
    public void cancel(@NotNull GrimPlugin plugin) {
        bukkitScheduler.cancelTasks(GrimACBukkitLoaderPlugin.PLUGIN);
    }
}
