package ac.grim.grimac.platform.bukkit.scheduler.bukkit;

import ac.grim.grimac.platform.api.scheduler.PlatformScheduler;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;

@Getter
public class BukkitPlatformScheduler implements PlatformScheduler {
    private final @NonNull BukkitAsyncScheduler asyncScheduler = new BukkitAsyncScheduler();
    private final @NonNull BukkitGlobalRegionScheduler globalRegionScheduler = new BukkitGlobalRegionScheduler();
    private final @NonNull BukkitEntityScheduler entityScheduler = new BukkitEntityScheduler();
    private final @NonNull BukkitRegionScheduler regionScheduler = new BukkitRegionScheduler();
}
