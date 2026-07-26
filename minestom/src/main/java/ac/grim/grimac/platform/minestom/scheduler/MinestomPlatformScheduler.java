package ac.grim.grimac.platform.minestom.scheduler;

import ac.grim.grimac.platform.api.scheduler.AsyncScheduler;
import ac.grim.grimac.platform.api.scheduler.EntityScheduler;
import ac.grim.grimac.platform.api.scheduler.GlobalRegionScheduler;
import ac.grim.grimac.platform.api.scheduler.PlatformScheduler;
import ac.grim.grimac.platform.api.scheduler.RegionScheduler;
import org.jetbrains.annotations.NotNull;

/** Aggregates the Minestom scheduler implementations behind Grim's {@link PlatformScheduler}. */
public final class MinestomPlatformScheduler implements PlatformScheduler {

    private final MinestomAsyncScheduler async = new MinestomAsyncScheduler();
    private final MinestomGlobalRegionScheduler global = new MinestomGlobalRegionScheduler();
    private final MinestomRegionScheduler region = new MinestomRegionScheduler(global);
    private final MinestomEntityScheduler entity = new MinestomEntityScheduler(global);

    @Override
    public @NotNull AsyncScheduler getAsyncScheduler() {
        return async;
    }

    @Override
    public @NotNull GlobalRegionScheduler getGlobalRegionScheduler() {
        return global;
    }

    @Override
    public @NotNull EntityScheduler getEntityScheduler() {
        return entity;
    }

    @Override
    public @NotNull RegionScheduler getRegionScheduler() {
        return region;
    }
}
