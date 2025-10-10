package ac.grim.grimac.events;

import ac.grim.grimac.api.plugin.GrimPlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages the resolution of platform-specific plugin contexts into the universal {@link GrimPlugin} interface.
 * <p>
 * This class is the bridge that allows the API to remain platform-agnostic while still supporting
 * platform-specific objects for lifecycle management.
 */
@ApiStatus.Internal
public final class GrimExtensionManager {
    private final List<GrimExtensionResolver> resolvers = new CopyOnWriteArrayList<>();
    private ResolutionFailureHandler failureHandler = failedContext ->
            new IllegalArgumentException("Unable to resolve plugin context for type: " + failedContext.getClass().getName() +
                    ". Ensure you are passing a valid platform plugin instance or a pre-existing GrimPlugin.");

    /**
     * Sets a custom handler for creating exceptions when plugin context resolution fails.
     * This allows platform-specific implementations to provide more descriptive error messages.
     *
     * @param failureHandler The handler to use.
     */
    public void setFailureHandler(@NotNull ResolutionFailureHandler failureHandler) {
        this.failureHandler = Objects.requireNonNull(failureHandler, "failureHandler cannot be null");
    }

    /**
     * Registers a new resolver. This is intended to be called by the core GrimAC implementation
     * during its startup phase.
     *
     * @param resolver The resolver to add.
     */
    public void registerResolver(@NotNull GrimExtensionResolver resolver) {
        Objects.requireNonNull(resolver, "resolver cannot be null");
        this.resolvers.add(resolver);
    }

    /**
     * Resolves a context object into a GrimPlugin by trying all registered resolvers in order.
     * <p>
     * It also has built-in logic to handle being passed a {@link GrimPlugin} instance directly.
     *
     * @param context The context object to resolve.
     * @return The resolved GrimPlugin.
     * @throws IllegalArgumentException if no registered resolver can handle the provided context type.
     */
    @NotNull
    public GrimPlugin getPlugin(@NotNull Object context) {
        Objects.requireNonNull(context, "context cannot be null");

        // First, check for the universal case: was a GrimPlugin already passed?
        if (context instanceof GrimPlugin) {
            return (GrimPlugin) context;
        }

        // Next, iterate through platform-specific resolvers.
        for (GrimExtensionResolver resolver : resolvers) {
            GrimPlugin resolved = resolver.resolve(context);
            if (resolved != null) {
                return resolved;
            }
        }

        // If no resolver succeeded, throw a descriptive error.
        throw this.failureHandler.createExceptionFor(context);
    }
}
