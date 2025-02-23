package ac.grim.bukkit.utils.scheduler.folia;

import ac.grim.grimac.platform.api.scheduler.TaskHandle;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

public class FoliaTaskHandle implements TaskHandle {

    private final ScheduledTask task;

    public FoliaTaskHandle(ScheduledTask task) {
        this.task = task;
    }

    @Override
    public boolean isSync() {
        return false;
    }

    @Override
    public boolean getCancelled() {
        return task.isCancelled();
    }

    @Override
    public void cancel() {
        task.cancel();
    }
}
