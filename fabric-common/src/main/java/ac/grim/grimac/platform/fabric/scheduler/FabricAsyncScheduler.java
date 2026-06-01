package ac.grim.grimac.platform.fabric.scheduler;

import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.platform.api.scheduler.AsyncScheduler;
import ac.grim.grimac.platform.api.scheduler.PlatformScheduler;
import ac.grim.grimac.platform.api.scheduler.TaskHandle;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class FabricAsyncScheduler implements AsyncScheduler {

    // Bukkit's runTaskAsynchronously() hands work to a shared, growable thread pool
    // (CraftAsyncScheduler uses a ThreadPoolExecutor, core 4 / max unbounded), and its scheduler is
    // explicitly built for dispatch from ANY thread: CraftScheduler tracks live tasks in a
    // ConcurrentHashMap (runners), mints ids with an AtomicInteger, and enqueues through a lock-free
    // AtomicReference tail. So callers may schedule/cancel off the main thread. We mirror that
    // contract: a pooled ScheduledExecutorService for execution and a ConcurrentHashMap for tracking.
    private final ScheduledExecutorService executor;
    private final Map<Future<?>, GrimPlugin> tasks = new ConcurrentHashMap<>();

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
