package ac.grim.grimac.platform.fabric.scheduler;

import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.platform.api.scheduler.AsyncScheduler;
import ac.grim.grimac.platform.api.scheduler.PlatformScheduler;
import ac.grim.grimac.platform.api.scheduler.TaskHandle;
import ac.grim.grimac.utils.data.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FabricAsyncScheduler implements AsyncScheduler {
    private final Map<Thread, Pair<GrimPlugin, Runnable>> asyncTasks = new HashMap<>();
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
        asyncTasks.put(thread, new Pair<>(plugin, cancellationTask));
        thread.start();
        return new FabricTaskHandle(cancellationTask, false);
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
        asyncTasks.put(thread, new Pair<>(plugin, cancellationTask));
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
        asyncTasks.put(thread, new Pair<>(plugin, cancellationTask));
        thread.start();
        return new FabricTaskHandle(cancellationTask, false); // false for async
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return runAtFixedRate(plugin, task,
                PlatformScheduler.convertTicksToTime(initialDelayTicks, TimeUnit.MILLISECONDS),
                PlatformScheduler.convertTicksToTime(periodTicks, TimeUnit.MILLISECONDS),
            TimeUnit.MILLISECONDS); // Convert ticks to milliseconds
    }

    @Override
    public void cancel(@NotNull GrimPlugin plugin) {
        // Cancel tasks only for the specified plugin
        Iterator<Map.Entry<Thread, Pair<GrimPlugin, Runnable>>> iterator = asyncTasks.entrySet().iterator();
        List<Runnable> cancellationTasks = new ArrayList<>();

        while (iterator.hasNext()) {
            Map.Entry<Thread, Pair<GrimPlugin, Runnable>> entry = iterator.next();
            if (entry.getValue().first().equals(plugin)) {
                cancellationTasks.add(entry.getValue().second());
                iterator.remove();
            }
        }

        for (Runnable cancellationTask : cancellationTasks) {
            cancellationTask.run();
        }
    }

    public void cancelAll() {
        List<Runnable> cancellationTasks = asyncTasks.values().stream()
                .map(Pair::second)
                .toList();
        asyncTasks.clear();

        for (Runnable cancellationTask : cancellationTasks) {
            cancellationTask.run();
        }
    }
}
