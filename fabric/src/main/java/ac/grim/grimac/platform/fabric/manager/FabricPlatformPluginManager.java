package ac.grim.grimac.platform.fabric.manager;

import ac.grim.grimac.api.event.EventBus;
import ac.grim.grimac.api.event.GrimEvent;
import ac.grim.grimac.api.event.OptimizedEventBus;
import ac.grim.grimac.platform.api.PlatformPlugin;
import ac.grim.grimac.platform.api.manager.PlatformPluginManager;
import ac.grim.grimac.platform.api.permission.Permission;
import ac.grim.grimac.platform.fabric.FabricPlatformPlugin;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.util.Collection;
import java.util.Optional;

public class FabricPlatformPluginManager implements PlatformPluginManager {
    private final EventBus eventBus = new OptimizedEventBus();

    @Override
    public void callEvent(GrimEvent event) {
        eventBus.post(event);
    }

    @Override
    public EventBus getEventBus() {
        return this.eventBus;
    }

    @Override
    public PlatformPlugin[] getPlugins() {
        // Get all loaded mods from Fabric Loader
        Collection<ModContainer> mods = FabricLoader.getInstance().getAllMods();
        PlatformPlugin[] plugins = new PlatformPlugin[mods.size()];
        int i = 0;
        for (ModContainer mod : mods) {
            plugins[i++] = new FabricPlatformPlugin(mod);
        }
        return plugins;
    }

    @Override
    public PlatformPlugin getPlugin(String pluginName) {
        // Look up a mod by its ID
        Optional<ModContainer> mod = FabricLoader.getInstance().getModContainer(pluginName);
        return mod.map(FabricPlatformPlugin::new).orElse(null);
    }

    // TODO implement this
    @Override
    public Permission getPermission(String permissionName) {
        return null;
    }

    @Override
    public void addPermission(Permission permission) {}

    @Override
    public boolean isPluginEnabled(String pluginName) {
        PlatformPlugin plugin = getPlugin(pluginName);
        return plugin != null && plugin.isEnabled();
    }
}
