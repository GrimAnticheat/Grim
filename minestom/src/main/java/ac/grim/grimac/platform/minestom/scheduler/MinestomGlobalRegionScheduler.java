package ac.grim.grimac.platform.minestom.scheduler;

import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.platform.api.scheduler.GlobalRegionScheduler;
import ac.grim.grimac.platform.api.scheduler.TaskHandle;
import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

/**
 * Runs tasks on Minestom's main tick scheduler. On a non-region server this is also what
 * the region/entity schedulers delegate to.
 */
public final class MinestomGlobalRegionScheduler implements GlobalRegionScheduler {

    private static Scheduler scheduler() {
        return MinecraftServer.getSchedulerManager();
    }

    private static TaskSchedule ticks(long ticks) {
        return TaskSchedule.tick((int) Math.max(1, ticks));
    }

    @Override
    public void execute(@NotNull GrimPlugin plugin, @NotNull Runnable task) {
        scheduler().buildTask(task).schedule();
    }

    @Override
    public TaskHandle run(@NotNull GrimPlugin plugin, @NotNull Runnable task) {
        return MinestomTaskHandle.sync(scheduler().buildTask(task).schedule());
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimPlugin plugin, @NotNull Runnable task, long delay) {
        return MinestomTaskHandle.sync(scheduler().buildTask(task).delay(ticks(delay)).schedule());
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return MinestomTaskHandle.sync(scheduler().buildTask(task)
                .delay(ticks(initialDelayTicks))
                .repeat(ticks(periodTicks))
                .schedule());
    }

    @Override
    public void cancel(@NotNull GrimPlugin plugin) {
        // Minestom's scheduler is global and not keyed by plugin; individual TaskHandles
        // returned above are cancellable. Single-tenant server → nothing to bulk-cancel.
    }
}
