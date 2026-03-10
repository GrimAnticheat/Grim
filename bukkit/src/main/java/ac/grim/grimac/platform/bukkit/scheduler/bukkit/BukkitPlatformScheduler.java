package ac.grim.grimac.platform.bukkit.scheduler.bukkit;

import ac.grim.grimac.platform.api.scheduler.PlatformScheduler;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class BukkitPlatformScheduler implements PlatformScheduler {
    private final @NotNull BukkitAsyncScheduler asyncScheduler = new BukkitAsyncScheduler();
    private final @NotNull BukkitGlobalRegionScheduler globalRegionScheduler = new BukkitGlobalRegionScheduler();
    private final @NotNull BukkitEntityScheduler entityScheduler = new BukkitEntityScheduler();
    private final @NotNull BukkitRegionScheduler regionScheduler = new BukkitRegionScheduler();
}
