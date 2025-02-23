package ac.grim.bukkit.utils.scheduler.folia;

import ac.grim.grimac.platform.api.scheduler.*;

public class FoliaPlatformScheduler implements PlatformScheduler {

    private final FoliaAsyncScheduler foliaAsyncScheduler = new FoliaAsyncScheduler();
    private final FoliaGlobalRegionScheduler foliaGlobalRegionScheduler = new FoliaGlobalRegionScheduler();
    private final FoliaEntityScheduler foliaEntityScheduler = new FoliaEntityScheduler();
    private final FoliaRegionScheduler foliaRegionScheduler = new FoliaRegionScheduler();

    @Override
    public AsyncScheduler getAsyncScheduler() {
        return foliaAsyncScheduler;
    }

    @Override
    public GlobalRegionScheduler getGlobalRegionScheduler() {
        return foliaGlobalRegionScheduler;
    }

    @Override
    public EntityScheduler getEntityScheduler() {
        return foliaEntityScheduler;
    }

    @Override
    public RegionScheduler getRegionScheduler() {
        return foliaRegionScheduler;
    }
}
