package net.kyori.adventure.util;

import org.jetbrains.annotations.NotNull;

/**
 * Compatibility bridge for Adventure 4.x libraries (such as PacketEvents)
 * running on an Adventure 5.x runtime environment.
 *
 * In Adventure 5.x, {@code net.kyori.adventure.util.Buildable$Builder} was removed,
 * which causes {@link NoClassDefFoundError} when libraries compiled against Adventure 4.x
 * load their builder classes.
 */
public interface Buildable {

    interface Builder<R> {
        @NotNull R build();
    }
}
