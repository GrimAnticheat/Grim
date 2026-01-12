package ac.grim.grimac.manager;

import ac.grim.grimac.manager.tick.Tickable;
import ac.grim.grimac.manager.tick.impl.ClearRecentlyUpdatedBlocks;
import ac.grim.grimac.manager.tick.impl.ClientVersionSetter;
import ac.grim.grimac.manager.tick.impl.ResetTick;
import ac.grim.grimac.manager.tick.impl.TickInventory;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class TickManager {
    public int currentTick;
    private final ClassToInstanceMap<Tickable> syncTick;
    private final ClassToInstanceMap<Tickable> asyncTick;

    // Lock: Stores the Thread currently running the task
    private final AtomicReference<Thread> asyncRunner = new AtomicReference<>(null);
    private volatile long asyncStartTime = 0;

    public TickManager() {
        syncTick = new ImmutableClassToInstanceMap.Builder<Tickable>()
                .put(ResetTick.class, new ResetTick())
                .build();

        asyncTick = new ImmutableClassToInstanceMap.Builder<Tickable>()
                .put(ClientVersionSetter.class, new ClientVersionSetter())
                .put(TickInventory.class, new TickInventory())
                .put(ClearRecentlyUpdatedBlocks.class, new ClearRecentlyUpdatedBlocks())
                .build();
    }

    public void tickSync() {
        currentTick++;
        syncTick.values().forEach(Tickable::tick);
    }

    public void tickAsync() {
        Thread previous = asyncRunner.get();

        // 1. SAFETY: If a thread is already running, we SKIP this tick to prevent the spiral.
        if (previous != null && previous.isAlive()) {
            // 2. DEBUG: If it has been stuck for > 1 second, tell us WHY.
            long duration = System.currentTimeMillis() - asyncStartTime;
            if (duration > 1000) {
                LogUtil.warn("[GrimAC-DEBUG] Async Tick STUCK for " + duration + "ms!");
                LogUtil.warn("Holding Thread: " + previous.getName() + " (ID: " + previous.getId() + ")");

                // Print the Stack Trace of the stuck thread
                StackTraceElement[] stack = previous.getStackTrace();
                String trace = Arrays.stream(stack)
                        .limit(15) // Top 15 lines are usually enough
                        .map(StackTraceElement::toString)
                        .collect(Collectors.joining("\n\tat "));
                LogUtil.warn("STUCK AT:\n\tat " + trace);
            }
            return;
        }

        // Acquire Lock
        asyncRunner.set(Thread.currentThread());
        asyncStartTime = System.currentTimeMillis();

        try {
            asyncTick.values().forEach(Tickable::tick);
        } catch (Throwable t) {
            LogUtil.error("Fatal error in async tick loop", t);
        } finally {
            // Release Lock
            asyncRunner.set(null);
        }
    }
}
