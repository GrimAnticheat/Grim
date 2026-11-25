package ac.grim.grimac.platform.bukkit;

import ac.grim.grimac.platform.api.PlatformPlugin;
import lombok.RequiredArgsConstructor;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class BukkitPlatformPlugin implements PlatformPlugin {
    private final @NotNull Plugin plugin;

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
