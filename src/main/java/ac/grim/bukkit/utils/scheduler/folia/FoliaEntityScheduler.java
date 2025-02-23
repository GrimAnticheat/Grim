package ac.grim.bukkit.utils.scheduler.folia;

import ac.grim.bukkit.GrimACBukkitLoaderPlugin;
import ac.grim.bukkit.entity.BukkitGrimEntity;
import ac.grim.grimac.api.GrimPlugin;
import ac.grim.grimac.platform.api.entity.GrimEntity;
import ac.grim.grimac.platform.api.scheduler.EntityScheduler;
import ac.grim.grimac.platform.api.scheduler.TaskHandle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FoliaEntityScheduler implements EntityScheduler {

    @Override
    public void execute(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable run, @Nullable Runnable retired, long delay) {
        ((BukkitGrimEntity) entity).getBukkitEntity().getScheduler().execute(GrimACBukkitLoaderPlugin.PLUGIN, run, () -> {}, delay);
    }

    @Override
    public TaskHandle run(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired) {
        return new FoliaTaskHandle(
                ((BukkitGrimEntity) entity).getBukkitEntity().getScheduler().run(GrimACBukkitLoaderPlugin.PLUGIN, (ignored) -> task.run(), () -> {})
        );
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired, long delayTicks) {
        return new FoliaTaskHandle(
                ((BukkitGrimEntity) entity).getBukkitEntity().getScheduler().runDelayed(GrimACBukkitLoaderPlugin.PLUGIN, (ignored) -> task.run(), () -> {}, delayTicks)
        );
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired, long initialDelayTicks, long periodTicks) {
        return new FoliaTaskHandle(
                ((BukkitGrimEntity) entity).getBukkitEntity().getScheduler().runAtFixedRate(GrimACBukkitLoaderPlugin.PLUGIN, (ignored) -> task.run(), () -> {}, initialDelayTicks, periodTicks)
        );
    }
}
