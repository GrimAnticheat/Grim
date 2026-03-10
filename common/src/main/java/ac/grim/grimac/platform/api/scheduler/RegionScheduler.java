/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2024 retrooper and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ac.grim.grimac.platform.api.scheduler;

import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.platform.api.world.PlatformWorld;
import ac.grim.grimac.utils.math.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a scheduler for executing region tasks
 */
public interface RegionScheduler {

    /**
     * Schedules a task to be executed on the region which owns the location.
     *
     * @param plugin The plugin that owns the task
     * @param world  The world of the region that owns the task
     * @param chunkX The chunk X coordinate of the region that owns the task
     * @param chunkZ The chunk Z coordinate of the region that owns the task
     * @param task   The task to execute
     */
    void execute(@NotNull GrimPlugin plugin, @NotNull PlatformWorld world, int chunkX, int chunkZ, @NotNull Runnable task);

    /**
     * Schedules a task to be executed on the region which owns the location.
     *
     * @param plugin   The plugin that owns the task
     * @param location The location at which the region executing should own
     * @param task     The task to execute
     */
    void execute(@NotNull GrimPlugin plugin, @NotNull Location location, @NotNull Runnable task);

    /**
     * Schedules a task to be executed on the region which owns the location on the next tick.
     *
     * @param plugin The plugin that owns the task
     * @param world  The world of the region that owns the task
     * @param chunkX The chunk X coordinate of the region that owns the task
     * @param chunkZ The chunk Z coordinate of the region that owns the task
     * @param task   The task to execute
     * @return {@link TaskHandle} instance representing a wrapped task
     */
    TaskHandle run(@NotNull GrimPlugin plugin, @NotNull PlatformWorld world, int chunkX, int chunkZ, @NotNull Runnable task);

    /**
     * Schedules a task to be executed on the region which owns the location on the next tick.
     *
     * @param plugin   The plugin that owns the task
     * @param location The location at which the region executing should own
     * @param task     The task to execute
     * @return {@link TaskHandle} instance representing a wrapped task
     */
    TaskHandle run(@NotNull GrimPlugin plugin, @NotNull Location location, @NotNull Runnable task);

    /**
     * Schedules a task to be executed on the region which owns the location after the specified delay in ticks.
     *
     * @param plugin     The plugin that owns the task
     * @param world      The world of the region that owns the task
     * @param chunkX     The chunk X coordinate of the region that owns the task
     * @param chunkZ     The chunk Z coordinate of the region that owns the task
     * @param task       The task to execute
     * @param delayTicks The delay, in ticks before the method is invoked. Any value less-than 1 may throw an error.
     * @return {@link TaskHandle} instance representing a wrapped task
     */
    TaskHandle runDelayed(@NotNull GrimPlugin plugin, @NotNull PlatformWorld world, int chunkX, int chunkZ, @NotNull Runnable task, long delayTicks);

    /**
     * Schedules a task to be executed on the region which owns the location after the specified delay in ticks.
     *
     * @param plugin     The plugin that owns the task
     * @param location   The location at which the region executing should own
     * @param task       The task to execute
     * @param delayTicks The delay, in ticks before the method is invoked. Any value less-than 1 may throw an error.
     * @return {@link TaskHandle} instance representing a wrapped task
     */
    TaskHandle runDelayed(@NotNull GrimPlugin plugin, @NotNull Location location, @NotNull Runnable task, long delayTicks);

    /**
     * Schedules a repeating task to be executed on the region which owns the location after the initial delay with the specified period.
     *
     * @param plugin            The plugin that owns the task
     * @param world             The world of the region that owns the task
     * @param chunkX            The chunk X coordinate of the region that owns the task
     * @param chunkZ            The chunk Z coordinate of the region that owns the task
     * @param task              The task to execute
     * @param initialDelayTicks The initial delay, in ticks before the method is invoked. Any value less-than 1 may throw an error.
     * @param periodTicks       The period, in ticks. Any value less-than 1 may throw an error.
     * @return {@link TaskHandle} instance representing a wrapped task
     */
    TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull PlatformWorld world, int chunkX, int chunkZ, @NotNull Runnable task, long initialDelayTicks, long periodTicks);

    /**
     * Schedules a repeating task to be executed on the region which owns the location after the initial delay with the specified period.
     *
     * @param plugin            The plugin that owns the task
     * @param location          The location at which the region executing should own
     * @param task              The task to execute
     * @param initialDelayTicks The initial delay, in ticks before the method is invoked. Any value less-than 1 may throw an error.
     * @param periodTicks       The period, in ticks. Any value less-than 1 may throw an error.
     * @return {@link TaskHandle} instance representing a wrapped task
     */
    TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Location location, @NotNull Runnable task, long initialDelayTicks, long periodTicks);
}
