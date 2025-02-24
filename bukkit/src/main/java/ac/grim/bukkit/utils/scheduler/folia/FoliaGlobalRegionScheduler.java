package ac.grim.bukkit.utils.scheduler.folia;

import ac.grim.bukkit.GrimACBukkitLoaderPlugin;
import ac.grim.grimac.api.GrimPlugin;
import ac.grim.grimac.platform.api.scheduler.GlobalRegionScheduler;
import ac.grim.grimac.platform.api.scheduler.TaskHandle;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class FoliaGlobalRegionScheduler implements GlobalRegionScheduler {

    private final io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler globalRegionScheduler = Bukkit.getGlobalRegionScheduler();

    @Override
    public void execute(@NotNull GrimPlugin plugin, @NotNull Runnable run) {
        globalRegionScheduler.execute(GrimACBukkitLoaderPlugin.PLUGIN, run);
    }

    @Override
    public TaskHandle run(@NotNull GrimPlugin plugin, @NotNull Runnable task) {
        return new FoliaTaskHandle(globalRegionScheduler.run(GrimACBukkitLoaderPlugin.PLUGIN, (ignored) -> task.run()));
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimPlugin plugin, @NotNull Runnable task, long delay) {
        return new FoliaTaskHandle(globalRegionScheduler.runDelayed(GrimACBukkitLoaderPlugin.PLUGIN, (ignored) -> task.run(), delay));
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return new FoliaTaskHandle(globalRegionScheduler.runAtFixedRate(GrimACBukkitLoaderPlugin.PLUGIN, (ignored) -> task.run(), initialDelayTicks, periodTicks));
    }

    @Override
    public void cancel(@NotNull GrimPlugin plugin) {
        globalRegionScheduler.cancelTasks(GrimACBukkitLoaderPlugin.PLUGIN);
    }
}
