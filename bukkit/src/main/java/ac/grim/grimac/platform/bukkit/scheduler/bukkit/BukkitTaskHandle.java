package ac.grim.grimac.platform.bukkit.scheduler.bukkit;

import ac.grim.grimac.platform.api.scheduler.TaskHandle;
import org.bukkit.scheduler.BukkitTask;

public class BukkitTaskHandle implements TaskHandle {

    private final BukkitTask bukkitTask;

    public BukkitTaskHandle(BukkitTask bukkitTask) {
        this.bukkitTask = bukkitTask;
    }

    @Override
    public boolean isSync() {
        return bukkitTask.isSync();
    }

    @Override
    public boolean isCancelled() {
        return bukkitTask.isCancelled();
    }

    @Override
    public void cancel() {
        bukkitTask.cancel();
    }
}
