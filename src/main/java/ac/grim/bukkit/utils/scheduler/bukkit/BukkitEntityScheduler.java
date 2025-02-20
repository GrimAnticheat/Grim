package ac.grim.bukkit.utils.scheduler.bukkit;

import ac.grim.bukkit.GrimACBukkitLoaderPlugin;
import ac.grim.grimac.api.GrimPlugin;
import ac.grim.grimac.entity.GrimEntity;
import ac.grim.grimac.utils.scheduler.EntityScheduler;
import ac.grim.grimac.utils.scheduler.TaskHandle;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BukkitEntityScheduler implements EntityScheduler {

    private final BukkitScheduler bukkitScheduler = Bukkit.getScheduler();

    @Override
    public void execute(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable run, @Nullable Runnable retired, long delay) {
        bukkitScheduler.runTaskLater(GrimACBukkitLoaderPlugin.PLUGIN, run, delay);
    }

    @Override
    public TaskHandle run(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired) {
        return new BukkitTaskHandle(bukkitScheduler.runTask(GrimACBukkitLoaderPlugin.PLUGIN, task));
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired, long delayTicks) {
        return new BukkitTaskHandle(bukkitScheduler.runTaskLater(GrimACBukkitLoaderPlugin.PLUGIN, task, delayTicks));
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired, long initialDelayTicks, long periodTicks) {
        return new BukkitTaskHandle(bukkitScheduler.runTaskTimer(GrimACBukkitLoaderPlugin.PLUGIN, task, initialDelayTicks, periodTicks));
    }
}
