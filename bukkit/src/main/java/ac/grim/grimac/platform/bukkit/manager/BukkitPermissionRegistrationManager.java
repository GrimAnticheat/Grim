package ac.grim.grimac.platform.bukkit.manager;

import ac.grim.grimac.platform.api.manager.PermissionRegistrationManager;
import ac.grim.grimac.platform.api.permissions.PermissionDefaultValue;
import ac.grim.grimac.platform.bukkit.utils.convert.BukkitConversionUtils;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;

public class BukkitPermissionRegistrationManager implements PermissionRegistrationManager {
    /**
     * Registers a permission with the specified default value on Bukkit.
     * This method is only called for dynamic permissions (e.g., check-specific permissions
     * like "grim.exempt.checkname") that are generated at runtime. Most other static permissions
     * (e.g., "grim.exempt", "grim.alerts.enable-on-join") are registered with their defaults
     * in the `plugin.yml` file, which is defined in the Bukkit Gradle build script.
     *
     * <p>Dynamic permissions are registered here to ensure they are available for autocomplete
     * and permission checks immediately on startup. If the permission already exists, its default
     * value is updated to match the specified value.</p>
     *
     * @param name         The permission node to register (e.g., "grim.exempt.checkname").
     * @param defaultValue The default value for the permission.
     */
    @Override
    public void registerPermission(String name, PermissionDefaultValue defaultValue) {
        final Permission bukkitPermission = Bukkit.getPluginManager().getPermission(name);
        if (bukkitPermission == null) {
            Bukkit.getPluginManager().addPermission(new Permission(name, BukkitConversionUtils.toBukkitPermissionDefault(defaultValue)));
        } else {
            bukkitPermission.setDefault(BukkitConversionUtils.toBukkitPermissionDefault(defaultValue));
        }
    }
}
