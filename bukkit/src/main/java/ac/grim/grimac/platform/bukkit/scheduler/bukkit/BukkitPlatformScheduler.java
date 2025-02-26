package ac.grim.grimac.platform.bukkit.scheduler.bukkit;

import ac.grim.grimac.platform.api.scheduler.AsyncScheduler;
import ac.grim.grimac.platform.api.scheduler.EntityScheduler;
import ac.grim.grimac.platform.api.scheduler.GlobalRegionScheduler;
import ac.grim.grimac.platform.api.scheduler.PlatformScheduler;
import ac.grim.grimac.platform.api.scheduler.RegionScheduler;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

public class BukkitPlatformScheduler implements PlatformScheduler {

    private final BukkitAsyncScheduler bukkitAsyncScheduler = new BukkitAsyncScheduler();
    private final BukkitGlobalRegionScheduler bukkitGlobalRegionScheduler = new BukkitGlobalRegionScheduler();
    private final BukkitEntityScheduler bukkitEntityScheduler = new BukkitEntityScheduler();
    private final BukkitRegionScheduler bukkitRegionScheduler = new BukkitRegionScheduler();

    @Override
    public @NonNull @NotNull AsyncScheduler getAsyncScheduler() {
        return bukkitAsyncScheduler;
    }

    @Override
    public @NonNull @NotNull GlobalRegionScheduler getGlobalRegionScheduler() {
        return bukkitGlobalRegionScheduler;
    }

    @Override
    public @NonNull @NotNull EntityScheduler getEntityScheduler() {
        return bukkitEntityScheduler;
    }

    @Override
    public @NonNull @NotNull RegionScheduler getRegionScheduler() {
        return bukkitRegionScheduler;
    }
}
