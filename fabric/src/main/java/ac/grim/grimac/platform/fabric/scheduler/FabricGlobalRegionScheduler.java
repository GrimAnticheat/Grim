package ac.grim.grimac.platform.fabric.scheduler;

import ac.grim.grimac.api.GrimPlugin;
import ac.grim.grimac.platform.api.scheduler.GlobalRegionScheduler;
import ac.grim.grimac.platform.api.scheduler.TaskHandle;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FabricGlobalRegionScheduler implements GlobalRegionScheduler {
    private final Map<FabricPlatformScheduler.ScheduledTask, Runnable> taskMap = new HashMap<>();
    private final GrimPlugin plugin;

    public FabricGlobalRegionScheduler(GrimPlugin plugin) {
        this.plugin = plugin;
        // Register the task handler to run on server tick
        ServerTickEvents.END_SERVER_TICK.register(this::handleTasks);
    }

    private void handleTasks(MinecraftServer server) {
        Iterator<FabricPlatformScheduler.ScheduledTask> iterator = taskMap.keySet().iterator();
        while (iterator.hasNext()) {
            FabricPlatformScheduler.ScheduledTask task = iterator.next();
            if (server.getTicks() >= task.nextRunTick) {
                try {
                    task.task.run();
                } catch (Exception e) {
                    plugin.getLogger().warning("Error executing scheduled task: " + e.getMessage());
                    e.printStackTrace();
                }
                if (task.isPeriodic) {
                    task.nextRunTick = server.getTicks() + task.period;
                } else {
                    iterator.remove();
                }
            }
        }
    }

    @Override
    public void execute(@NotNull GrimPlugin plugin, @NotNull Runnable run) {
        // Execute immediately on the main thread
        // Since this is a global region scheduler, we'll schedule it for the next tick
        FabricPlatformScheduler.ScheduledTask scheduledTask = new FabricPlatformScheduler.ScheduledTask(
                run,
                GrimACFabricLoaderPlugin.FABRIC_SERVER.getTicks(), // Run on current tick
                0,
                false,
                plugin
        );
        Runnable cancellationTask = () -> taskMap.remove(scheduledTask);
        taskMap.put(scheduledTask, cancellationTask);
    }

    @Override
    public TaskHandle run(@NotNull GrimPlugin plugin, @NotNull Runnable task) {
        FabricPlatformScheduler.ScheduledTask scheduledTask = new FabricPlatformScheduler.ScheduledTask(
                task,
                GrimACFabricLoaderPlugin.FABRIC_SERVER.getTicks(), // Run on current tick
                0,
                false,
                plugin
        );
        Runnable cancellationTask = () -> taskMap.remove(scheduledTask);
        taskMap.put(scheduledTask, cancellationTask);
        return new FabricTaskHandle(cancellationTask, true); // true for sync
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimPlugin plugin, @NotNull Runnable task, long delay) {
        FabricPlatformScheduler.ScheduledTask scheduledTask = new FabricPlatformScheduler.ScheduledTask(
                task,
                GrimACFabricLoaderPlugin.FABRIC_SERVER.getTicks() + delay,
                0,
                false,
                plugin
        );
        Runnable cancellationTask = () -> taskMap.remove(scheduledTask);
        taskMap.put(scheduledTask, cancellationTask);
        return new FabricTaskHandle(cancellationTask, true); // true for sync
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        FabricPlatformScheduler.ScheduledTask scheduledTask = new FabricPlatformScheduler.ScheduledTask(
                task,
                GrimACFabricLoaderPlugin.FABRIC_SERVER.getTicks() + initialDelayTicks,
                periodTicks,
                true,
                plugin
        );
        Runnable cancellationTask = () -> taskMap.remove(scheduledTask);
        taskMap.put(scheduledTask, cancellationTask);
        return new FabricTaskHandle(cancellationTask, true); // true for sync
    }

    @Override
    public void cancel(@NotNull GrimPlugin plugin) {
        FabricPlatformScheduler.cancelPluginTasks(taskMap, plugin);
    }

    // New method to cancel all tasks
    public void cancelAll() {
        FabricPlatformScheduler.cancelAllTasks(taskMap);
    }
}
