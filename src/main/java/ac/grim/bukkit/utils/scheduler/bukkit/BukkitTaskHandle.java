package ac.grim.bukkit.utils.scheduler.bukkit;

import ac.grim.grimac.utils.scheduler.TaskHandle;
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
    public boolean getCancelled() {
        return bukkitTask.isCancelled();
    }

    @Override
    public void cancel() {
        bukkitTask.cancel();
    }
}
