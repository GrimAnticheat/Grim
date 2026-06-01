package ac.grim.grimac.platform.fabric.scheduler;

import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.platform.api.scheduler.AsyncScheduler;
import ac.grim.grimac.platform.api.scheduler.PlatformScheduler;
import ac.grim.grimac.platform.api.scheduler.TaskHandle;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class FabricAsyncScheduler implements AsyncScheduler {

    // Bukkit's runTaskAsynchronously() hands work to a shared, reused thread pool rather than
    // creating a fresh thread per task. We mirror that with one scheduled pool so Grim's async
    // work (a handful of runNow / runAtFixedRate callers) reuses threads instead of leaking a new
    // one for every task. As before, this task map is only touched while scheduling/cancelling on
    // the main thread (the pool threads never touch it), so a plain HashMap is sufficient.
    private final ScheduledExecutorService executor;
    private final Map<Future<?>, GrimPlugin> tasks = new HashMap<>();

    public FabricAsyncScheduler() {
        AtomicInteger threadCount = new AtomicInteger();
        this.executor = Executors.newScheduledThreadPool(
                Math.max(4, Runtime.getRuntime().availableProcessors() / 2),
                runnable -> {
                    Thread thread = new Thread(runnable, "Grim-Async-" + threadCount.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                });
    }

    @Override
    public TaskHandle runNow(@NotNull GrimPlugin plugin, @NotNull Runnable task) {
        return track(plugin, executor.submit(task));
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimPlugin plugin, @NotNull Runnable task, long delay, @NotNull TimeUnit timeUnit) {
        return track(plugin, executor.schedule(task, delay, timeUnit));
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Runnable task, long delay, long period, @NotNull TimeUnit timeUnit) {
        return track(plugin, executor.scheduleAtFixedRate(task, delay, period, timeUnit));
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return runAtFixedRate(plugin, task,
                PlatformScheduler.convertTicksToTime(initialDelayTicks, TimeUnit.MILLISECONDS),
                PlatformScheduler.convertTicksToTime(periodTicks, TimeUnit.MILLISECONDS),
                TimeUnit.MILLISECONDS);
    }

    private TaskHandle track(GrimPlugin plugin, Future<?> future) {
        tasks.put(future, plugin);
        return new FabricTaskHandle(() -> {
            future.cancel(true);
            tasks.remove(future);
        }, false);
    }

    @Override
    public void cancel(@NotNull GrimPlugin plugin) {
        tasks.entrySet().removeIf(entry -> {
            if (entry.getValue().equals(plugin)) {
                entry.getKey().cancel(true);
                return true;
            }
            return false;
        });
    }

    public void cancelAll() {
        tasks.keySet().forEach(future -> future.cancel(true));
        tasks.clear();
    }
}
