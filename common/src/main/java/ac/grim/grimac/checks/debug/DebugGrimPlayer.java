package ac.grim.grimac.checks.debug;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.player.User;
import org.jetbrains.annotations.NotNull;

public class DebugGrimPlayer extends GrimPlayer {

    private static final long THRESHOLD_NANO = 200_000_000L; // 200ms
    private static final long THRESHOLD_MILLIS = 200;

    // Global Offsets: Volatile so all threads see the pause adjustment immediately.
    // When the Netty thread pauses for 60s, it updates this.
    // The Scheduler thread sees the update and "stops" the timeout check.
    private volatile long offsetNano = 0;
    private volatile long offsetMillis = 0;

    // ThreadLocal: Each thread tracks its OWN execution time.
    // This prevents the Scheduler Thread (pollData) from resetting the timer
    // while the Netty Thread is sitting at a breakpoint.
    private final ThreadLocal<Long> packetStartNano = ThreadLocal.withInitial(System::nanoTime);
    private final ThreadLocal<Long> packetStartMillis = ThreadLocal.withInitial(System::currentTimeMillis);

    public DebugGrimPlayer(@NotNull User user) {
        super(user, System.currentTimeMillis(), System.nanoTime());
    }

    @Override
    public void processingPacket() {
        // Reset the timer ONLY for the current thread (Netty or Scheduler)
        this.packetStartNano.set(System.nanoTime());
        this.packetStartMillis.set(System.currentTimeMillis());
    }

    @Override
    public long nano() {
        // SAFETY CHECK:
        // If something in the parent constructor calls nano() BEFORE we are fully initialized,
        // packetStartNano will be null. We must fallback to System time to prevent NPE.
        if (packetStartNano == null) return System.nanoTime();

        long sysTime = System.nanoTime();

        // 1. Get start time for THIS thread
        long start = packetStartNano.get();
        long executionTime = sysTime - start;

        // 2. Check if THIS thread was paused/stuck
        if (executionTime > THRESHOLD_NANO) {
            // Update the global offset so ALL threads know to skip this time
            synchronized (this) {
                offsetNano += executionTime;
            }
            // Reset this thread's timer so we don't loop
            packetStartNano.set(sysTime);
        } else if (executionTime < 0) {
            // Self-healing for rare thread-local init edge cases or system clock jumps
            packetStartNano.set(sysTime);
        }

        // 3. Return System Time minus the Global Pause Offset
        return sysTime - offsetNano;
    }

    @Override
    public long now() {
        if (packetStartMillis == null) return System.currentTimeMillis();

        long sysTime = System.currentTimeMillis();
        long start = packetStartMillis.get();
        long executionTime = sysTime - start;

        if (executionTime > THRESHOLD_MILLIS) {
            synchronized (this) {
                offsetMillis += executionTime;
            }
            packetStartMillis.set(sysTime);
        }

        return sysTime - offsetMillis;
    }
}
