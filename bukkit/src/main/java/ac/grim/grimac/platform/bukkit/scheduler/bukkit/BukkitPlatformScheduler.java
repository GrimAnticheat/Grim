package ac.grim.grimac.platform.bukkit.scheduler.bukkit;

import ac.grim.grimac.platform.api.scheduler.AsyncScheduler;
import ac.grim.grimac.platform.api.scheduler.EntityScheduler;
import ac.grim.grimac.platform.api.scheduler.GlobalRegionScheduler;
import ac.grim.grimac.platform.api.scheduler.PlatformScheduler;
import ac.grim.grimac.platform.api.scheduler.RegionScheduler;
import org.checkerframework.checker.nullness.qual.NonNull;

public class BukkitPlatformScheduler implements PlatformScheduler {

    private final BukkitAsyncScheduler bukkitAsyncScheduler = new BukkitAsyncScheduler();
    private final BukkitGlobalRegionScheduler bukkitGlobalRegionScheduler = new BukkitGlobalRegionScheduler();
    private final BukkitEntityScheduler bukkitEntityScheduler = new BukkitEntityScheduler();
    private final BukkitRegionScheduler bukkitRegionScheduler = new BukkitRegionScheduler();

    @Override
    public @NonNull AsyncScheduler getAsyncScheduler() {
        return bukkitAsyncScheduler;
    }

    @Override
    public @NonNull GlobalRegionScheduler getGlobalRegionScheduler() {
        return bukkitGlobalRegionScheduler;
    }

    @Override
    public @NonNull EntityScheduler getEntityScheduler() {
        return bukkitEntityScheduler;
    }

    @Override
    public @NonNull RegionScheduler getRegionScheduler() {
        return bukkitRegionScheduler;
    }
}
