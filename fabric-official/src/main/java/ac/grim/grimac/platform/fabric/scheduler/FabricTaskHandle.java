package ac.grim.grimac.platform.fabric.scheduler;

import ac.grim.grimac.platform.api.scheduler.TaskHandle;
import lombok.Getter;

public class FabricTaskHandle implements TaskHandle {
    private final Runnable cancellationTask;
    @Getter
    private boolean cancelled;
    @Getter
    private final boolean sync;

    public FabricTaskHandle(Runnable cancellationTask) {
        this.cancellationTask = cancellationTask;
        this.sync = false;
    }

    public FabricTaskHandle(Runnable cancellationTask, boolean sync) {
        this.cancellationTask = cancellationTask;
        this.sync = sync;
    }

    @Override
    public void cancel() {
        this.cancellationTask.run();
        this.cancelled = true;
    }
}
