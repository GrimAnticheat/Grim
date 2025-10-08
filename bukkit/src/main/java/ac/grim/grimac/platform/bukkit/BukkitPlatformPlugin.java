package ac.grim.grimac.platform.bukkit;

import ac.grim.grimac.platform.api.PlatformPlugin;
import org.bukkit.plugin.Plugin;

public class BukkitPlatformPlugin implements PlatformPlugin {
    private final Plugin plugin;

    public BukkitPlatformPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isEnabled() {
        return plugin.isEnabled();
    }

    @Override
    public String getName() {
        return plugin.getName();
    }

    @SuppressWarnings("deprecation")
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }
}
