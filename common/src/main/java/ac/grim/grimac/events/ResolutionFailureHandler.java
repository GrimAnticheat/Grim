package ac.grim.grimac.events;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ResolutionFailureHandler {
    /**
     * Creates a runtime exception to be thrown when resolution fails.
     *
     * @param failedContext The object that could not be resolved.
     * @return The exception to be thrown.
     */
    RuntimeException createExceptionFor(@NotNull Object failedContext);
}
