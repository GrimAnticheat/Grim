package ac.grim.bukkit;

import ac.grim.grimac.platform.api.PlatformPlugin;
import org.bukkit.plugin.Plugin;

public class BukkitPlatformPlugin implements PlatformPlugin {
    private final Plugin plugin;

    public BukkitPlatformPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String isEnabled() {
        return String.valueOf(plugin.isEnabled());
    }

    @Override
    public String getName() {
        return plugin.getName();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }
}
