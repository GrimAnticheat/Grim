package ac.grim.grimac.platform.fabric.scheduler;

import ac.grim.grimac.api.GrimPlugin;
import ac.grim.grimac.platform.api.scheduler.AsyncScheduler;
import ac.grim.grimac.platform.api.scheduler.TaskHandle;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FabricAsyncScheduler implements AsyncScheduler {
    private final Map<Thread, Runnable> asyncTasks = new HashMap<>();
    private final GrimPlugin plugin;

    public FabricAsyncScheduler(GrimPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public TaskHandle runNow(@NotNull GrimPlugin plugin, @NotNull Runnable task) {
        Thread thread = new Thread(task);
        Runnable cancellationTask = () -> {
            thread.interrupt();
            asyncTasks.remove(thread);
        };
        asyncTasks.put(thread, cancellationTask);
        thread.start();
        return new FabricTaskHandle(cancellationTask, false); // false for async
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimPlugin plugin, @NotNull Runnable task, long delay, @NotNull TimeUnit timeUnit) {
        long delayMillis = timeUnit.toMillis(delay);
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(delayMillis);
                task.run();
            } catch (InterruptedException e) {
                // Handle interruption
            }
        });
        Runnable cancellationTask = () -> {
            thread.interrupt();
            asyncTasks.remove(thread);
        };
        asyncTasks.put(thread, cancellationTask);
        thread.start();
        return new FabricTaskHandle(cancellationTask, false); // false for async
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Runnable task, long delay, long period, @NotNull TimeUnit timeUnit) {
        long delayMillis = timeUnit.toMillis(delay);
        long periodMillis = timeUnit.toMillis(period);
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(delayMillis);
                while (!Thread.currentThread().isInterrupted()) {
                    task.run();
                    Thread.sleep(periodMillis);
                }
            } catch (InterruptedException e) {
                // Handle interruption
            }
        });
        Runnable cancellationTask = () -> {
            thread.interrupt();
            asyncTasks.remove(thread);
        };
        asyncTasks.put(thread, cancellationTask);
        thread.start();
        return new FabricTaskHandle(cancellationTask, false); // false for async
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return runAtFixedRate(plugin, task, initialDelayTicks, periodTicks, TimeUnit.MILLISECONDS); // Convert ticks to milliseconds
    }

    @Override
    public void cancel(@NotNull GrimPlugin plugin) {
        FabricPlatformScheduler.cancelAllTasks(asyncTasks);
    }
}
