package ac.grim.bukkit.utils.scheduler.bukkit;

import ac.grim.grimac.platform.api.scheduler.*;

public class BukkitPlatformScheduler implements PlatformScheduler {

    private final BukkitAsyncScheduler bukkitAsyncScheduler = new BukkitAsyncScheduler();
    private final BukkitGlobalRegionScheduler bukkitGlobalRegionScheduler = new BukkitGlobalRegionScheduler();
    private final BukkitEntityScheduler bukkitEntityScheduler = new BukkitEntityScheduler();
    private final BukkitRegionScheduler bukkitRegionScheduler = new BukkitRegionScheduler();

    @Override
    public AsyncScheduler getAsyncScheduler() {
        return bukkitAsyncScheduler;
    }

    @Override
    public GlobalRegionScheduler getGlobalRegionScheduler() {
        return bukkitGlobalRegionScheduler;
    }

    @Override
    public EntityScheduler getEntityScheduler() {
        return bukkitEntityScheduler;
    }

    @Override
    public RegionScheduler getRegionScheduler() {
        return bukkitRegionScheduler;
    }
}
