package ac.grim.grimac.utils.scheduler;

import java.util.concurrent.TimeUnit;

public interface PlatformScheduler {
    public AsyncScheduler getAsyncScheduler();
    public GlobalRegionScheduler getGlobalRegionScheduler();
    public EntityScheduler getEntityScheduler();
    public RegionScheduler getRegionScheduler();

    /**
     * Converts the specified time to ticks.
     *
     * @param time     The time to convert.
     * @param timeUnit The time unit of the time.
     * @return The time converted to ticks.
     */
    public static long convertTimeToTicks(long time, TimeUnit timeUnit) {
        return timeUnit.toMillis(time) / 50;
    }
}
