package ac.grim.bukkit.manager;

import ac.grim.bukkit.BukkitPlatformPlugin;
import ac.grim.bukkit.utils.convert.ConversionUtils;
import ac.grim.grimac.api.event.EventBus;
import ac.grim.grimac.api.event.GrimEvent;
import ac.grim.grimac.api.event.OptimizedEventBus;
import ac.grim.grimac.platform.api.PlatformPlugin;
import ac.grim.grimac.platform.api.manager.PlatformPluginManager;
import ac.grim.grimac.platform.api.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class BukkitPlatformPluginManager implements PlatformPluginManager {

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
        Plugin[] bukkitPlugins = Bukkit.getPluginManager().getPlugins();
        PlatformPlugin[] plugins = new PlatformPlugin[bukkitPlugins.length];

        for (int i = 0; i < bukkitPlugins.length; i++) {
            plugins[i] = new BukkitPlatformPlugin(bukkitPlugins[i]);
        }

        return plugins;
    }

    @Override
    public PlatformPlugin getPlugin(String pluginName) {
        return new BukkitPlatformPlugin(Bukkit.getPluginManager().getPlugin(pluginName));
    }

    @Override
    public Permission getPermission(String permissionName) {
        org.bukkit.permissions.Permission bukkitPermission = Bukkit.getPluginManager().getPermission(permissionName);
        if (bukkitPermission == null) return null;
        return new Permission(permissionName, ConversionUtils.fromBukkitPermissionDefault(bukkitPermission.getDefault()));
    }

    @Override
    public void addPermission(Permission permission) {
        Bukkit.getPluginManager().addPermission(new org.bukkit.permissions.Permission(
                permission.getName(),
                ConversionUtils.toBukkitPermissionDefault(permission.getDefault())
        ));
    }
}
