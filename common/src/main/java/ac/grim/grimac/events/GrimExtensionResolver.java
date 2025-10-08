package ac.grim.grimac.events;

import ac.grim.grimac.api.plugin.GrimPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * A functional interface responsible for attempting to resolve a generic context object
 * into a {@link GrimPlugin}.
 * <p>
 * Implementations of this are provided by the core GrimAC platform module (e.g., for Bukkit, Fabric)
 * and registered with the central {@link GrimPluginManager}.
 */
@FunctionalInterface
public interface GrimExtensionResolver {

    /**
     * Attempts to resolve the given context object into a GrimPlugin.
     *
     * @param context The context object to resolve (e.g., a Bukkit Plugin, a Plugin Class, a Fabric Mod).
     * @return An Optional containing the resolved GrimPlugin if this resolver supports the context type,
     *         otherwise an empty Optional.
     */
    @Nullable GrimPlugin resolve(@NotNull Object context);

}
