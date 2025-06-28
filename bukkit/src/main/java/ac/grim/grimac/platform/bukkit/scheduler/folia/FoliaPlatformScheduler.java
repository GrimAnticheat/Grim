package ac.grim.grimac.platform.bukkit.scheduler.folia;

import ac.grim.grimac.platform.api.scheduler.PlatformScheduler;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;

@Getter
public class FoliaPlatformScheduler implements PlatformScheduler {
    private final @NonNull FoliaAsyncScheduler asyncScheduler = new FoliaAsyncScheduler();
    private final @NonNull FoliaGlobalRegionScheduler globalRegionScheduler = new FoliaGlobalRegionScheduler();
    private final @NonNull FoliaEntityScheduler entityScheduler = new FoliaEntityScheduler();
    private final @NonNull FoliaRegionScheduler regionScheduler = new FoliaRegionScheduler();
}
