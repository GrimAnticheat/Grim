package ac.grim.grimac.platform.minestom.scheduler;

import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.platform.api.scheduler.RegionScheduler;
import ac.grim.grimac.platform.api.scheduler.TaskHandle;
import ac.grim.grimac.platform.api.world.PlatformWorld;
import ac.grim.grimac.utils.math.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Minestom is not region-threaded — everything runs on the single tick thread — so region
 * scheduling collapses to the {@link MinestomGlobalRegionScheduler}; the world/chunk/location
 * arguments carry no threading meaning here.
 */
public final class MinestomRegionScheduler implements RegionScheduler {

    private final MinestomGlobalRegionScheduler global;

    public MinestomRegionScheduler(MinestomGlobalRegionScheduler global) {
        this.global = global;
    }

    @Override
    public void execute(@NotNull GrimPlugin plugin, @NotNull PlatformWorld world, int chunkX, int chunkZ, @NotNull Runnable task) {
        global.execute(plugin, task);
    }

    @Override
    public void execute(@NotNull GrimPlugin plugin, @NotNull Location location, @NotNull Runnable task) {
        global.execute(plugin, task);
    }

    @Override
    public TaskHandle run(@NotNull GrimPlugin plugin, @NotNull PlatformWorld world, int chunkX, int chunkZ, @NotNull Runnable task) {
        return global.run(plugin, task);
    }

    @Override
    public TaskHandle run(@NotNull GrimPlugin plugin, @NotNull Location location, @NotNull Runnable task) {
        return global.run(plugin, task);
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimPlugin plugin, @NotNull PlatformWorld world, int chunkX, int chunkZ, @NotNull Runnable task, long delayTicks) {
        return global.runDelayed(plugin, task, delayTicks);
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimPlugin plugin, @NotNull Location location, @NotNull Runnable task, long delayTicks) {
        return global.runDelayed(plugin, task, delayTicks);
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull PlatformWorld world, int chunkX, int chunkZ, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return global.runAtFixedRate(plugin, task, initialDelayTicks, periodTicks);
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Location location, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return global.runAtFixedRate(plugin, task, initialDelayTicks, periodTicks);
    }
}
