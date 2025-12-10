package ac.grim.grimac.platform.fabric.scheduler;

import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.platform.api.scheduler.GlobalRegionScheduler;
import ac.grim.grimac.platform.api.scheduler.TaskHandle;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FabricGlobalRegionScheduler implements GlobalRegionScheduler {
    // TODO (Cross-platform) (Threading) try to make this not Concurrent
    private final Map<FabricPlatformScheduler.ScheduledTask, Runnable> taskMap = new ConcurrentHashMap<>();
    private final GrimPlugin plugin;

    public FabricGlobalRegionScheduler(GrimPlugin plugin) {
        this.plugin = plugin;
        // Register the task handler to run on server tick
        ServerTickEvents.END_SERVER_TICK.register(this::handleTasks);
    }

    private void handleTasks(MinecraftServer server) {
        FabricPlatformScheduler.handleSyncTasks(taskMap, server, plugin);
    }

    @Override
    public void execute(@NotNull GrimPlugin plugin, @NotNull Runnable run) {
        run(plugin, run);
    }

    @Override
    public TaskHandle run(@NotNull GrimPlugin plugin, @NotNull Runnable task) {
        return runDelayed(plugin, task, 0);
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimPlugin plugin, @NotNull Runnable task, long delay) {
        FabricPlatformScheduler.ScheduledTask scheduledTask = new FabricPlatformScheduler.ScheduledTask(
                task,
                GrimACFabricLoaderPlugin.FABRIC_SERVER.getTickCount() + delay,
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
                GrimACFabricLoaderPlugin.FABRIC_SERVER.getTickCount() + initialDelayTicks,
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
