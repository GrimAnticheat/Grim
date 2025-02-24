package ac.grim.bukkit.utils.scheduler.folia;

import ac.grim.bukkit.GrimACBukkitLoaderPlugin;
import ac.grim.bukkit.world.BukkitPlatformWorld;
import ac.grim.grimac.api.GrimPlugin;
import ac.grim.grimac.platform.api.scheduler.RegionScheduler;
import ac.grim.grimac.platform.api.scheduler.TaskHandle;
import ac.grim.grimac.utils.math.Location;
import ac.grim.grimac.platform.api.world.PlatformWorld;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class FoliaRegionScheduler implements RegionScheduler {

    private final io.papermc.paper.threadedregions.scheduler.RegionScheduler regionScheduler = Bukkit.getRegionScheduler();

    @Override
    public void execute(@NotNull GrimPlugin plugin, @NotNull PlatformWorld world, int chunkX, int chunkZ, @NotNull Runnable run) {
        regionScheduler.execute(GrimACBukkitLoaderPlugin.PLUGIN, ((BukkitPlatformWorld) world).getBukkitWorld(), chunkX, chunkZ, run);
    }

    @Override
    public void execute(@NotNull GrimPlugin plugin, @NotNull Location location, @NotNull Runnable run) {
        execute(plugin, location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4, run);

    }

    @Override
    public TaskHandle run(@NotNull GrimPlugin plugin, @NotNull PlatformWorld world, int chunkX, int chunkZ, @NotNull Runnable task) {
        return new FoliaTaskHandle(regionScheduler.run(GrimACBukkitLoaderPlugin.PLUGIN, ((BukkitPlatformWorld) world).getBukkitWorld(), chunkX, chunkZ, (ignored) -> task.run()));
    }

    @Override
    public TaskHandle run(@NotNull GrimPlugin plugin, @NotNull Location location, @NotNull Runnable task) {
        return run(plugin, location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4, task);
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimPlugin plugin, @NotNull PlatformWorld world, int chunkX, int chunkZ, @NotNull Runnable task, long delayTicks) {
        return new FoliaTaskHandle(regionScheduler.runDelayed(GrimACBukkitLoaderPlugin.PLUGIN,
                ((BukkitPlatformWorld) world).getBukkitWorld(), chunkX, chunkZ, (ignored) -> task.run(), delayTicks));
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimPlugin plugin, @NotNull Location location, @NotNull Runnable task, long delayTicks) {
        return runDelayed(plugin, location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4, task, delayTicks);
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull PlatformWorld world, int chunkX, int chunkZ, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return new FoliaTaskHandle(regionScheduler.runAtFixedRate(GrimACBukkitLoaderPlugin.PLUGIN,
                ((BukkitPlatformWorld) world).getBukkitWorld(), chunkX, chunkZ, (ignored) -> task.run(), initialDelayTicks, periodTicks));
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Location location, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return runAtFixedRate(plugin, location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4, task, initialDelayTicks, periodTicks);
    }
}
