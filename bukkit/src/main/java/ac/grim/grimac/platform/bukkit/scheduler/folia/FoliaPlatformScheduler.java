package ac.grim.grimac.platform.bukkit.scheduler.folia;

import ac.grim.grimac.platform.api.scheduler.AsyncScheduler;
import ac.grim.grimac.platform.api.scheduler.EntityScheduler;
import ac.grim.grimac.platform.api.scheduler.GlobalRegionScheduler;
import ac.grim.grimac.platform.api.scheduler.PlatformScheduler;
import ac.grim.grimac.platform.api.scheduler.RegionScheduler;
import org.checkerframework.checker.nullness.qual.NonNull;

public class FoliaPlatformScheduler implements PlatformScheduler {

    private final FoliaAsyncScheduler foliaAsyncScheduler = new FoliaAsyncScheduler();
    private final FoliaGlobalRegionScheduler foliaGlobalRegionScheduler = new FoliaGlobalRegionScheduler();
    private final FoliaEntityScheduler foliaEntityScheduler = new FoliaEntityScheduler();
    private final FoliaRegionScheduler foliaRegionScheduler = new FoliaRegionScheduler();

    @Override
    public @NonNull AsyncScheduler getAsyncScheduler() {
        return foliaAsyncScheduler;
    }

    @Override
    public @NonNull GlobalRegionScheduler getGlobalRegionScheduler() {
        return foliaGlobalRegionScheduler;
    }

    @Override
    public @NonNull EntityScheduler getEntityScheduler() {
        return foliaEntityScheduler;
    }

    @Override
    public @NonNull RegionScheduler getRegionScheduler() {
        return foliaRegionScheduler;
    }
}
