package ac.grim.grimac.manager;

import ac.grim.grimac.manager.tick.Tickable;
import ac.grim.grimac.manager.tick.impl.ClearRecentlyUpdatedBlocks;
import ac.grim.grimac.manager.tick.impl.ClientVersionSetter;
import ac.grim.grimac.manager.tick.impl.ResetTick;
import ac.grim.grimac.manager.tick.impl.TickInventory;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;

import java.util.concurrent.atomic.AtomicInteger;

public class TickManager {
    // Overflows after 4 years of uptime
    public int currentTick;
    private final ClassToInstanceMap<Tickable> syncTick;
    private final ClassToInstanceMap<Tickable> asyncTick;
    private final AtomicInteger activeAsyncTasks = new AtomicInteger(0);

    public TickManager() {
        syncTick = new ImmutableClassToInstanceMap.Builder<Tickable>()
                .put(ResetTick.class, new ResetTick())
                .build();

        asyncTick = new ImmutableClassToInstanceMap.Builder<Tickable>()
                .put(ClientVersionSetter.class, new ClientVersionSetter()) // Async because permission lookups might take a while, depending on the plugin
                .put(TickInventory.class, new TickInventory()) // Async because I've never gotten an exception from this.  It's probably safe.
                .put(ClearRecentlyUpdatedBlocks.class, new ClearRecentlyUpdatedBlocks())
                .build();
    }

    public void tickSync() {
        currentTick++;
        syncTick.values().forEach(Tickable::tick);
    }

    public void tickAsync() {
        int currentActive = activeAsyncTasks.incrementAndGet();
        long start = System.nanoTime();

        // LOG 1: Detect overlap immediately
        if (currentActive > 1) {
            LogUtil.warn("[DIAGNOSTIC] Async Overlap Detected! Active Threads: " + currentActive);
        }

        try {
            asyncTick.values().forEach(Tickable::tick);
        } finally {
            long duration = System.nanoTime() - start;
            int remaining = activeAsyncTasks.decrementAndGet();

            // LOG 2: Detect slow ticks (anything over 50ms means we are skipping ticks or stacking threads)
            if (duration > 50_000_000) { // 50ms in nanoseconds
                LogUtil.warn("[DIAGNOSTIC] Slow Async Tick: " + (duration / 1_000_000) + "ms. Remaining Threads: " + remaining);
            }
        }
    }
}
