package ac.grim.grimac.platform.bukkit.scheduler.folia;

import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.platform.api.entity.GrimEntity;
import ac.grim.grimac.platform.api.scheduler.EntityScheduler;
import ac.grim.grimac.platform.api.scheduler.TaskHandle;
import ac.grim.grimac.platform.bukkit.GrimACBukkitLoaderPlugin;
import ac.grim.grimac.platform.bukkit.entity.BukkitGrimEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FoliaEntityScheduler implements EntityScheduler {

    @Override
    public void execute(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable run, @Nullable Runnable retired, long delay) {
        ((BukkitGrimEntity) entity).getBukkitEntity().getScheduler().execute(GrimACBukkitLoaderPlugin.LOADER, run, retired, delay);
    }

    @Override
    public TaskHandle run(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired) {
        return new FoliaTaskHandle(((BukkitGrimEntity) entity).getBukkitEntity().getScheduler().run(GrimACBukkitLoaderPlugin.LOADER, (ignored) -> task.run(), retired));
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired, long delayTicks) {
        return new FoliaTaskHandle(
                ((BukkitGrimEntity) entity).getBukkitEntity().getScheduler().runDelayed(GrimACBukkitLoaderPlugin.LOADER, (ignored) -> task.run(), retired, delayTicks)
        );
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired, long initialDelayTicks, long periodTicks) {
        return new FoliaTaskHandle(((BukkitGrimEntity) entity).getBukkitEntity().getScheduler().runAtFixedRate(GrimACBukkitLoaderPlugin.LOADER, (ignored) -> task.run(), retired, initialDelayTicks, periodTicks));
    }
}
