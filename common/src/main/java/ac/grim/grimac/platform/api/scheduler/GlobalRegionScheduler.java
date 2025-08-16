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
import org.jetbrains.annotations.NotNull;

/**
 * Represents a scheduler for executing global region tasks.
 */
public interface GlobalRegionScheduler {

    /**
     * Schedules a task to be executed on the global region.
     *
     * @param plugin The plugin that owns the task
     * @param task   The task to execute
     */
    void execute(@NotNull GrimPlugin plugin, @NotNull Runnable task);

    /**
     * Schedules a task to be executed on the global region.
     *
     * @param plugin The plugin that owns the task
     * @param task   The task to execute
     * @return {@link TaskHandle} instance representing a wrapped task
     */
    TaskHandle run(@NotNull GrimPlugin plugin, @NotNull Runnable task);

    /**
     * Schedules a task to be executed on the global region after the specified delay in ticks.
     *
     * @param plugin The plugin that owns the task
     * @param task   The task to execute
     * @param delay  The delay, in ticks before the method is invoked. Any value less-than 1 may throw an error.
     * @return {@link TaskHandle} instance representing a wrapped task
     */
    TaskHandle runDelayed(@NotNull GrimPlugin plugin, @NotNull Runnable task, long delay);

    /**
     * Schedules a repeating task to be executed on the global region after the initial delay with the specified period.
     *
     * @param plugin            The plugin that owns the task
     * @param task              The task to execute
     * @param initialDelayTicks The initial delay, in ticks before the method is invoked. Any value less-than 1 may throw an error.
     * @param periodTicks       The period, in ticks. Any value less-than 1 may throw an error.
     * @return {@link TaskHandle} instance representing a wrapped task
     */
    TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Runnable task, long initialDelayTicks, long periodTicks);

    /**
     * Attempts to cancel all tasks scheduled by the specified plugin.
     *
     * @param plugin Specified plugin.
     */
    void cancel(@NotNull GrimPlugin plugin);
}
