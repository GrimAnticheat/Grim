package ac.grim.grimac.platform.api.manager;

import ac.grim.grimac.platform.api.PlatformPlugin;

public interface PlatformPluginManager {

    PlatformPlugin[] getPlugins();

    PlatformPlugin getPlugin(String pluginName);

    boolean isPluginEnabled(String pluginName);
}
