package ac.grim.grimac.platform.minestom.scheduler;

import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.platform.api.entity.GrimEntity;
import ac.grim.grimac.platform.api.scheduler.EntityScheduler;
import ac.grim.grimac.platform.api.scheduler.TaskHandle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * On Minestom all entities live on the single tick thread, so entity-affine scheduling
 * delegates to the {@link MinestomGlobalRegionScheduler}.
 * <p>
 * TODO Phase 3: honour {@code retired} — run it when the target entity is removed before the
 * task fires (Bukkit/Folia semantics). For now the task runs on the tick thread regardless.
 */
public final class MinestomEntityScheduler implements EntityScheduler {

    private final MinestomGlobalRegionScheduler global;

    public MinestomEntityScheduler(MinestomGlobalRegionScheduler global) {
        this.global = global;
    }

    @Override
    public void execute(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable run, @Nullable Runnable retired, long delay) {
        if (delay <= 0) {
            global.execute(plugin, run);
        } else {
            global.runDelayed(plugin, run, delay);
        }
    }

    @Override
    public TaskHandle run(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired) {
        return global.run(plugin, task);
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired, long delayTicks) {
        return global.runDelayed(plugin, task, delayTicks);
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired, long initialDelayTicks, long periodTicks) {
        return global.runAtFixedRate(plugin, task, initialDelayTicks, periodTicks);
    }
}
