package ac.grim.grimac.platform.bukkit.scheduler.bukkit;

import ac.grim.grimac.platform.api.scheduler.TaskHandle;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class BukkitTaskHandle implements TaskHandle {

    private final @NotNull BukkitTask task;

    @Contract(pure = true)
    public BukkitTaskHandle(@NotNull BukkitTask task) {
        this.task = Objects.requireNonNull(task);
    }

    @Override
    public boolean isSync() {
        return task.isSync();
    }

    @Override
    public boolean isCancelled() {
        return task.isCancelled();
    }

    @Override
    public void cancel() {
        task.cancel();
    }
}
