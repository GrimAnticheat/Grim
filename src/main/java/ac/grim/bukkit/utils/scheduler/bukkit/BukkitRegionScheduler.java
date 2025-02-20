package ac.grim.bukkit.utils.scheduler.bukkit;

import ac.grim.bukkit.GrimACBukkitLoaderPlugin;
import ac.grim.grimac.api.GrimPlugin;
import ac.grim.grimac.utils.scheduler.RegionScheduler;
import ac.grim.grimac.utils.scheduler.TaskHandle;
import ac.grim.grimac.world.Location;
import ac.grim.grimac.world.PlatformWorld;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

public class BukkitRegionScheduler implements RegionScheduler {

    private final BukkitScheduler bukkitScheduler = Bukkit.getScheduler();

    @Override
    public void execute(@NotNull GrimPlugin plugin, @NotNull PlatformWorld world, int chunkX, int chunkZ, @NotNull Runnable run) {
        bukkitScheduler.runTask(GrimACBukkitLoaderPlugin.PLUGIN, run);
    }

    @Override
    public void execute(@NotNull GrimPlugin plugin, @NotNull Location location, @NotNull Runnable run) {
        bukkitScheduler.runTask(GrimACBukkitLoaderPlugin.PLUGIN, run);
    }

    @Override
    public TaskHandle run(@NotNull GrimPlugin plugin, @NotNull PlatformWorld world, int chunkX, int chunkZ, @NotNull Runnable task) {
        return new BukkitTaskHandle(bukkitScheduler.runTask(GrimACBukkitLoaderPlugin.PLUGIN, task));
    }

    @Override
    public TaskHandle run(@NotNull GrimPlugin plugin, @NotNull Location location, @NotNull Runnable task) {
        return new BukkitTaskHandle(bukkitScheduler.runTask(GrimACBukkitLoaderPlugin.PLUGIN, task));
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimPlugin plugin, @NotNull PlatformWorld world, int chunkX, int chunkZ, @NotNull Runnable task, long delayTicks) {
        return new BukkitTaskHandle(bukkitScheduler.runTaskLater(GrimACBukkitLoaderPlugin.PLUGIN, task, delayTicks));
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimPlugin plugin, @NotNull Location location, @NotNull Runnable task, long delayTicks) {
        return new BukkitTaskHandle(bukkitScheduler.runTaskLater(GrimACBukkitLoaderPlugin.PLUGIN, task, delayTicks));
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull PlatformWorld world, int chunkX, int chunkZ, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return new BukkitTaskHandle(bukkitScheduler.runTaskTimer(GrimACBukkitLoaderPlugin.PLUGIN, task, initialDelayTicks, periodTicks));
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Location location, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return new BukkitTaskHandle(bukkitScheduler.runTaskTimer(GrimACBukkitLoaderPlugin.PLUGIN, task, initialDelayTicks, periodTicks));
    }
}
