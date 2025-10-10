package ac.grim.grimac.platform.api.scheduler;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * A cross-platform scheduler for scheduling tasks across different Minecraft server platforms.
 * This interface provides a unified API for scheduling tasks asynchronously, globally, or tied to specific
 * regions or entities, with platform-specific implementations for Folia, Bukkit (and forks), Fabric, and future platforms.
 * <p>
 * The scheduler is designed to abstract away platform-specific scheduling differences, allowing developers to write
 * platform-agnostic code. On Folia, the scheduler leverages advanced threading features like region-based and entity-based
 * scheduling. On non-Folia platforms (e.g., Bukkit, Fabric), region and entity scheduling behave synchronously, similar to
 * global region scheduling, ensuring compatibility and consistent behavior.
 * </p>
 * <p>
 * <b>Platform-Specific Behavior:</b>
 * <ul>
 *   <li><b>Folia:</b> Supports advanced threading features, including region-based scheduling (via {@link RegionScheduler}),
 *       entity-based scheduling (via {@link EntityScheduler}), asynchronous scheduling (via {@link AsyncScheduler}),
 *       and global region scheduling (via {@link GlobalRegionScheduler}).</li>
 *   <li><b>Non-Folia Platforms (Bukkit, Fabric, etc.):</b> Region and entity scheduling are implemented synchronously,
 *       behaving similarly to global region scheduling. Asynchronous scheduling is supported but may have platform-specific
 *       limitations.</li>
 * </ul>
 * </p>
 * <p>
 * <b>Usage Notes:</b>
 * <ul>
 *   <li>Use {@link #getAsyncScheduler()} for tasks that can run asynchronously, independent of the server tick process.</li>
 *   <li>Use {@link #getGlobalRegionScheduler()} for tasks that need to run on the global region (e.g., world time updates,
 *       weather cycles, console commands). On non-Folia platforms, this behaves like traditional synchronous scheduling.</li>
 *   <li>Use {@link #getRegionScheduler()} for tasks tied to specific locations. On Folia, this schedules tasks on the owning
 *       region thread. On non-Folia platforms, this behaves synchronously, similar to global region scheduling.</li>
 *   <li>Use {@link #getEntityScheduler()} for tasks tied to entities. On Folia, this ensures tasks follow the entity across
 *       regions and worlds, with proper handling of entity state changes. On non-Folia platforms, this behaves synchronously,
 *       similar to global region scheduling.</li>
 *   <li>Use {@link #convertTimeToTicks(long, TimeUnit)} to convert time values to ticks for scheduling.</li>
 * </ul>
 * </p>
 * <p>
 * <b>Cross-Platform Considerations:</b>
 * <ul>
 *   <li>On Folia, region and entity scheduling provide advanced threading benefits, such as improved performance and
 *       thread safety. On non-Folia platforms, these methods are implemented synchronously for compatibility.</li>
 *   <li>Developers should avoid using region or entity scheduling for tasks that do not require location or entity-specific
 *       behavior, especially on non-Folia platforms, as they offer no additional benefits over global region scheduling.</li>
 *   <li>Asynchronous scheduling is supported across all platforms, but the underlying implementation may vary. Ensure
 *       tasks scheduled asynchronously are thread-safe and do not access platform-specific APIs that require synchronous
 *       execution.</li>
 * </ul>
 * </p>
 * <p>
 * <b>Future Platforms:</b>
 * This scheduler is designed to be extensible, with support for additional platforms (e.g., Sponge, Velocity) planned for
 * the future. Platform-specific implementations will ensure consistent behavior, with fallbacks to synchronous scheduling
 * where advanced threading features are not available.
 * </p>
 */
public interface PlatformScheduler {
    /**
     * Converts the specified time to ticks.
     * <p>
     * This utility method converts a time value from the specified time unit to server ticks, where 1 tick = 50 milliseconds.
     * This is useful for scheduling tasks with delays or periods in ticks.
     * </p>
     * <p>
     * <b>Example:</b>
     * <pre>
     * long delayInTicks = PlatformScheduler.convertTimeToTicks(5, TimeUnit.SECONDS); // 5 seconds = 100 ticks
     * </pre>
     * </p>
     *
     * @param time     The time to convert.
     * @param timeUnit The time unit of the time.
     * @return The time converted to ticks.
     */
    static long convertTimeToTicks(long time, TimeUnit timeUnit) {
        return timeUnit.toMillis(time) / 50;
    }

    /**
     * Converts the specified number of ticks to a time value expressed in the
     * desired {@link java.util.concurrent.TimeUnit TimeUnit}.
     * <p>
     * Internally the method multiplies the given tick count by <code>50&nbsp;ms</code>
     * (duration of one tick) and then converts that millisecond value to the
     * requested unit.
     * </p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * long ticks = 100;                                   // 100 ticks
     * long seconds = PlatformScheduler.convertTicksToTime(ticks, TimeUnit.SECONDS);
     * // seconds == 5
     * </pre>
     *
     * @param ticks     The number of ticks to convert.
     * @param timeUnit  The unit in which you want the result.
     * @return          The converted time value in the requested unit.
     *
     * @see #convertTimeToTicks(long, TimeUnit)
     */
    static long convertTicksToTime(long ticks, TimeUnit timeUnit) {
        long millis = ticks * 50L;
        return timeUnit.convert(millis, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns the asynchronous task scheduler.
     * <p>
     * The async task scheduler can be used to schedule tasks that execute asynchronously from the server tick process.
     * These tasks are suitable for operations that do not require synchronization with the server tick, such as
     * database queries, file I/O, or other background processing.
     * </p>
     * <p>
     * <b>Platform-Specific Behavior:</b>
     * <ul>
     *   <li><b>Folia:</b> Uses Folia's async scheduler for true asynchronous execution.</li>
     *   <li><b>Non-Folia Platforms:</b> Uses platform-specific async scheduling (e.g., Bukkit's async tasks, Fabric's
     *       async executor). Ensure tasks are thread-safe, as async behavior may vary.</li>
     * </ul>
     * </p>
     * <p>
     * <b>Usage Notes:</b>
     * <ul>
     *   <li>Tasks scheduled asynchronously must be thread-safe and should not access platform-specific APIs that require
     *       synchronous execution (e.g., modifying world state, entity data).</li>
     *   <li>Use this scheduler for background tasks that do not need to interact with the server tick process.</li>
     * </ul>
     * </p>
     *
     * @return The async task scheduler.
     */
    @NotNull AsyncScheduler getAsyncScheduler();

    /**
     * Returns the global region task scheduler.
     * <p>
     * The global region scheduler can be used to schedule tasks to execute on the global region, which is responsible for
     * maintaining world day time, world game time, weather cycles, sleep night skipping, executing console commands, and
     * other miscellaneous tasks that do not belong to any specific region.
     * </p>
     * <p>
     * <b>Platform-Specific Behavior:</b>
     * <ul>
     *   <li><b>Folia:</b> Uses Folia's global region scheduler for tasks tied to the global region, executed on the
     *       appropriate thread.</li>
     *   <li><b>Non-Folia Platforms:</b> Behaves like traditional synchronous scheduling, executing tasks on the main
     *       server thread.</li>
     * </ul>
     * </p>
     * <p>
     * <b>Usage Notes:</b>
     * <ul>
     *   <li>Use this scheduler for tasks that need to run synchronously with the server tick process but are not tied to
     *       specific locations or entities.</li>
     *   <li>On non-Folia platforms, this is equivalent to traditional synchronous scheduling (e.g., Bukkit's scheduler,
     *       Fabric's server tick events).</li>
     * </ul>
     * </p>
     *
     * @return The global region scheduler.
     */
    @NotNull GlobalRegionScheduler getGlobalRegionScheduler();

    /**
     * Returns the entity task scheduler.
     * <p>
     * The entity scheduler is designed to schedule tasks tied to specific entities, ensuring that tasks are executed only
     * when the entity is contained in a world, on the owning thread for the region, and with proper handling of entity state
     * changes (e.g., teleportation, removal). This eliminates undefined behaviors resulting from entity state uncertainty.
     * </p>
     * <p>
     * <b>Platform-Specific Behavior:</b>
     * <ul>
     *   <li><b>Folia:</b> Uses Folia's entity scheduler, which follows the entity across regions and worlds, provides
     *       thread safety, and supports retired callbacks for tasks that cannot be executed due to entity removal.</li>
     *   <li><b>Non-Folia Platforms:</b> Behaves synchronously, similar to global region scheduling. Tasks are executed on
     *       the main server thread, without advanced threading features or entity state tracking.</li>
     * </ul>
     * </p>
     * <p>
     * <b>Usage Notes:</b>
     * <ul>
     *   <li>Use this scheduler for tasks that need to perform actions on entities, such as updating entity state, applying
     *       effects, or tracking entity movement.</li>
     *   <li>On Folia, this scheduler ensures tasks follow the entity across regions and worlds, with proper handling of
     *       entity state changes (e.g., teleportation, removal, or temporary inactivity). It also provides thread safety
     *       and supports retired callbacks for tasks that cannot be executed due to entity removal.</li>
     *   <li>On non-Folia platforms, this scheduler behaves synchronously, similar to global region scheduling. Tasks are
     *       executed on the main server thread, without advanced threading features or entity state tracking. Use this
     *       scheduler only when entity-specific behavior is required, as it offers no additional benefits over global
     *       region scheduling on non-Folia platforms.</li>
     *   <li>Ensure tasks scheduled with this scheduler are thread-safe on Folia, as they may execute on different threads.
     *       On non-Folia platforms, tasks are executed synchronously, so thread safety is not a concern.</li>
     * </ul>
     * </p>
     *
     * @return The entity task scheduler.
     */
    @NotNull EntityScheduler getEntityScheduler();

    /**
     * Returns the region task scheduler.
     * <p>
     * The region scheduler is designed to schedule tasks tied to specific locations, ensuring that tasks are executed on
     * the owning thread for the region that contains the location. This is particularly useful for operations that need to
     * interact with world state at specific coordinates, such as block updates or chunk modifications.
     * </p>
     * <p>
     * <b>Platform-Specific Behavior:</b>
     * <ul>
     *   <li><b>Folia:</b> Uses Folia's region scheduler, which schedules tasks on the owning thread for the region that
     *       contains the specified location. This provides improved performance and thread safety for region-specific
     *       operations.</li>
     *   <li><b>Non-Folia Platforms:</b> Behaves synchronously, similar to global region scheduling. Tasks are executed on
     *       the main server thread, without advanced threading features or region-specific optimization.</li>
     * </ul>
     * </p>
     * <p>
     * <b>Usage Notes:</b>
     * <ul>
     *   <li>Use this scheduler for tasks that need to perform actions at specific locations, such as updating blocks,
     *       modifying chunks, or interacting with world state at coordinates.</li>
     *   <li>On Folia, this scheduler ensures tasks are executed on the owning thread for the region, providing thread
     *       safety and improved performance for region-specific operations. Note that this scheduler is not suitable for
     *       entity-related tasks, as entities can move between regions; use {@link #getEntityScheduler()} for entity tasks.</li>
     *   <li>On non-Folia platforms, this scheduler behaves synchronously, similar to global region scheduling. Tasks are
     *       executed on the main server thread, without advanced threading features or region-specific optimization. Use
     *       this scheduler only when location-specific behavior is required, as it offers no additional benefits over
     *       global region scheduling on non-Folia platforms.</li>
     *   <li>Ensure tasks scheduled with this scheduler are thread-safe on Folia, as they may execute on different threads.
     *       On non-Folia platforms, tasks are executed synchronously, so thread safety is not a concern.</li>
     * </ul>
     * </p>
     *
     * @return The region task scheduler.
     */
    @NotNull RegionScheduler getRegionScheduler();
}
