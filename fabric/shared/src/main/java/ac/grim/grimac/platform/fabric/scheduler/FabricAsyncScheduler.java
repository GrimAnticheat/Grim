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
import java.util.function.Function;

public class FabricAsyncScheduler implements AsyncScheduler {

    // Bukkit's runTaskAsynchronously() hands work to a shared, growable thread pool
    // (CraftAsyncScheduler uses a ThreadPoolExecutor, core 4 / max unbounded), and its scheduler is
    // explicitly built for dispatch from ANY thread: CraftScheduler tracks live tasks in a
    // ConcurrentHashMap (runners), mints ids with an AtomicInteger, and enqueues through a lock-free
    // AtomicReference tail. So callers may schedule/cancel off the main thread. We mirror that
    // contract: a pooled ScheduledExecutorService for execution and a ConcurrentHashMap for tracking.
    private final ScheduledExecutorService executor;

    // Live tasks, kept so cancel(plugin)/cancelAll() can reach them. Keyed by a per-task token
    // rather than the Future so a one-shot task can drop itself the moment it finishes without
    // racing the Future being stored back here (the token is registered before the task can run).
    private final Map<Object, Tracked> tasks = new ConcurrentHashMap<>();

    private static final class Tracked {
        private final GrimPlugin plugin;
        private volatile Future<?> future;

        private Tracked(GrimPlugin plugin) {
            this.plugin = plugin;
        }
    }

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
        return schedule(plugin, task, true, body -> executor.submit(body));
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimPlugin plugin, @NotNull Runnable task, long delay, @NotNull TimeUnit timeUnit) {
        return schedule(plugin, task, true, body -> executor.schedule(body, delay, timeUnit));
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Runnable task, long delay, long period, @NotNull TimeUnit timeUnit) {
        return schedule(plugin, task, false, body -> executor.scheduleAtFixedRate(body, delay, period, timeUnit));
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return runAtFixedRate(plugin, task,
                PlatformScheduler.convertTicksToTime(initialDelayTicks, TimeUnit.MILLISECONDS),
                PlatformScheduler.convertTicksToTime(periodTicks, TimeUnit.MILLISECONDS),
                TimeUnit.MILLISECONDS);
    }

    // One-shot tasks remove themselves from the map once they finish, so it can't grow unbounded;
    // repeating tasks stay tracked until they're cancelled.
    private TaskHandle schedule(GrimPlugin plugin, Runnable task, boolean oneShot, Function<Runnable, Future<?>> submit) {
        Object token = new Object();
        Tracked tracked = new Tracked(plugin);
        tasks.put(token, tracked);

        Runnable body = oneShot ? () -> {
            try {
                task.run();
            } finally {
                tasks.remove(token);
            }
        } : task;

        tracked.future = submit.apply(body);

        return new FabricTaskHandle(() -> {
            Tracked removed = tasks.remove(token);
            if (removed != null && removed.future != null) {
                removed.future.cancel(true);
            }
        }, false);
    }

    @Override
    public void cancel(@NotNull GrimPlugin plugin) {
        tasks.values().removeIf(tracked -> {
            if (tracked.plugin.equals(plugin)) {
                Future<?> future = tracked.future;
                if (future != null) {
                    future.cancel(true);
                }
                return true;
            }
            return false;
        });
    }

    public void cancelAll() {
        tasks.values().forEach(tracked -> {
            Future<?> future = tracked.future;
            if (future != null) {
                future.cancel(true);
            }
        });
        tasks.clear();
    }
}
