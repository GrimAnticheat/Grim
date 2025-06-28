package ac.grim.grimac.platform.bukkit.scheduler.folia;

import ac.grim.grimac.platform.api.scheduler.TaskHandle;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class FoliaTaskHandle implements TaskHandle {

    private final @NotNull ScheduledTask task;

    @Contract(pure = true)
    public FoliaTaskHandle(@NotNull ScheduledTask task) {
        this.task = Objects.requireNonNull(task);
    }

    @Override
    public boolean isSync() {
        return false;
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
