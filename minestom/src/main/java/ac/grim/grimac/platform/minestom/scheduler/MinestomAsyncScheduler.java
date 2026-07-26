package ac.grim.grimac.platform.minestom.scheduler;

import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.platform.api.scheduler.AsyncScheduler;
import ac.grim.grimac.platform.api.scheduler.TaskHandle;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Off-tick scheduling on a small daemon pool (Minestom has no async region model). One tick
 * is treated as 50ms for the tick-based fixed-rate overload.
 */
public final class MinestomAsyncScheduler implements AsyncScheduler {

    private static final long MILLIS_PER_TICK = 50L;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "grim-async");
        thread.setDaemon(true);
        return thread;
    });

    private final CopyOnWriteArrayList<Future<?>> tracked = new CopyOnWriteArrayList<>();

    private TaskHandle track(Future<?> future) {
        tracked.add(future);
        return MinestomTaskHandle.async(future);
    }

    @Override
    public TaskHandle runNow(@NotNull GrimPlugin plugin, @NotNull Runnable task) {
        return track(executor.submit(task));
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimPlugin plugin, @NotNull Runnable task, long delay, @NotNull TimeUnit timeUnit) {
        return track(executor.schedule(task, delay, timeUnit));
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Runnable task, long delay, long period, @NotNull TimeUnit timeUnit) {
        return track(executor.scheduleAtFixedRate(task, delay, period, timeUnit));
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return track(executor.scheduleAtFixedRate(task,
                initialDelayTicks * MILLIS_PER_TICK, periodTicks * MILLIS_PER_TICK, TimeUnit.MILLISECONDS));
    }

    @Override
    public void cancel(@NotNull GrimPlugin plugin) {
        for (Future<?> future : tracked) {
            future.cancel(false);
        }
        tracked.clear();
    }
}
