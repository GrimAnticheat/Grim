package ac.grim.grimac.platform.minestom.scheduler;

import ac.grim.grimac.platform.api.scheduler.TaskHandle;
import net.minestom.server.timer.Task;

import java.util.concurrent.Future;
import java.util.function.BooleanSupplier;

/**
 * {@link TaskHandle} over either a Minestom {@link Task} (sync/tick tasks) or a
 * {@link Future} (async pool tasks).
 */
public final class MinestomTaskHandle implements TaskHandle {

    private final boolean sync;
    private final BooleanSupplier cancelledCheck;
    private final Runnable canceller;

    private MinestomTaskHandle(boolean sync, BooleanSupplier cancelledCheck, Runnable canceller) {
        this.sync = sync;
        this.cancelledCheck = cancelledCheck;
        this.canceller = canceller;
    }

    public static MinestomTaskHandle sync(Task task) {
        return new MinestomTaskHandle(true, () -> !task.isAlive(), task::cancel);
    }

    public static MinestomTaskHandle async(Future<?> future) {
        return new MinestomTaskHandle(false, future::isCancelled, () -> future.cancel(false));
    }

    @Override
    public boolean isSync() {
        return sync;
    }

    @Override
    public boolean isCancelled() {
        return cancelledCheck.getAsBoolean();
    }

    @Override
    public void cancel() {
        canceller.run();
    }
}
