package ac.grim.grimac.platform.minestom.manager;

import ac.grim.grimac.platform.api.PlatformPlugin;
import ac.grim.grimac.platform.api.manager.PlatformPluginManager;
import org.jetbrains.annotations.Nullable;

/**
 * Minestom has no plugin registry — features are wired directly into the server process — so
 * from Grim's perspective there are no third-party plugins to detect. (ViaVersion lives on the
 * proxy, not this backend, so its absence here is correct.)
 */
public final class MinestomPlatformPluginManager implements PlatformPluginManager {

    private static final PlatformPlugin[] NONE = new PlatformPlugin[0];

    @Override
    public PlatformPlugin[] getPlugins() {
        return NONE;
    }

    @Override
    public @Nullable PlatformPlugin getPlugin(String pluginName) {
        return null;
    }
}
