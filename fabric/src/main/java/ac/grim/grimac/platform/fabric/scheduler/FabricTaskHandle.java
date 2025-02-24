package ac.grim.grimac.platform.fabric.scheduler;

import ac.grim.grimac.platform.api.scheduler.TaskHandle;

public class FabricTaskHandle implements TaskHandle {
    private final Runnable cancellationTask;
    private boolean cancelled = false;
    private boolean isSync;

    public FabricTaskHandle(Runnable cancellationTask) {
        this.cancellationTask = cancellationTask;
    }

    public FabricTaskHandle(Runnable cancellationTask, boolean isSync) {
        this.cancellationTask = cancellationTask;
    }

    @Override
    public boolean isSync() {
        return this.isSync;
    }

    @Override
    public boolean getCancelled() {
        return this.cancelled;
    }

    @Override
    public void cancel() {
        this.cancellationTask.run();
        this.cancelled = true;
    }
}
