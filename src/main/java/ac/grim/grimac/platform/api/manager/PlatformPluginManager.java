package ac.grim.grimac.platform.api.manager;

import ac.grim.grimac.api.event.EventBus;
import ac.grim.grimac.api.event.GrimEvent;
import ac.grim.grimac.platform.api.permission.Permission;
import ac.grim.grimac.platform.api.PlatformPlugin;

public interface PlatformPluginManager {

    void callEvent(GrimEvent event);

    EventBus getEventBus();

    PlatformPlugin[] getPlugins();

    PlatformPlugin getPlugin(String pluginName);

    Permission getPermission(String permissionName);

    void addPermission(Permission permission);
}
