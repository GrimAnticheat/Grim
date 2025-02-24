package ac.grim.bukkit.utils.scheduler.folia;

import ac.grim.bukkit.GrimACBukkitLoaderPlugin;
import ac.grim.grimac.api.GrimPlugin;
import ac.grim.grimac.platform.api.scheduler.AsyncScheduler;
import ac.grim.grimac.platform.api.scheduler.TaskHandle;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class FoliaAsyncScheduler implements AsyncScheduler {

    private final io.papermc.paper.threadedregions.scheduler.AsyncScheduler asyncScheduler = Bukkit.getAsyncScheduler();

    @Override
    public TaskHandle runNow(@NotNull GrimPlugin plugin, @NotNull Runnable task) {
        return new FoliaTaskHandle(asyncScheduler.runNow(GrimACBukkitLoaderPlugin.PLUGIN, (ignored) -> task.run()));
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimPlugin plugin, @NotNull Runnable task, long delay, @NotNull TimeUnit timeUnit) {
        return new FoliaTaskHandle(
                asyncScheduler.runDelayed(
                    GrimACBukkitLoaderPlugin.PLUGIN,
                    (ignored) -> task.run(),
                    delay,
                    timeUnit
            )
        );
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Runnable task, long delay, long period, @NotNull TimeUnit timeUnit) {
        if (period < 1) period = 1;

        return new FoliaTaskHandle(
                asyncScheduler.runAtFixedRate(
                    GrimACBukkitLoaderPlugin.PLUGIN,
                    (ignored) -> task.run(),
                    delay,
                    period,
                    timeUnit
            )
        );
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        if (periodTicks < 1) periodTicks = 1;

        return new FoliaTaskHandle(
                asyncScheduler.runAtFixedRate(
                        GrimACBukkitLoaderPlugin.PLUGIN,
                        (ignored) -> task.run(),
                        initialDelayTicks * 50,
                        periodTicks * 50,
                        TimeUnit.MILLISECONDS
            )
        );
    }

    @Override
    public void cancel(@NotNull GrimPlugin plugin) {
        asyncScheduler.cancelTasks(GrimACBukkitLoaderPlugin.PLUGIN);
    }
}
