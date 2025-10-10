package ac.grim.grimac.platform.api.manager;

import ac.grim.grimac.platform.api.permissions.PermissionDefaultValue;

/**
 * Manages permissions across different platforms (e.g., Bukkit, Fabric).
 * Provides methods for registering permissions.
 */
public interface PermissionRegistrationManager {

    /**
     * Registers a permission with the specified default value.
     * This method ensures that the permission is registered with the server,
     * making it available for autocomplete immediately on startup.
     *
     * <p>Registering permissions on startup is important for ensuring that
     * dynamic permissions (e.g., check-specific permissions like "grim.exempt.checkname")
     * are available for autocomplete before Grim attempts to use them. This is
     * particularly useful for command systems and permission management plugins
     * that rely on registered permissions for autocomplete functionality.</p>
     *
     * <p>Note that modern permission plugins (e.g., LuckPerms) may register
     * autocompletions dynamically through player hasPermission calls, even
     * if the permission is not explicitly registered. However, explicitly
     * registering permissions on startup ensures that they are available for
     * autocomplete immediately, improving user experience and compatibility with
     * older or less dynamic permission systems.</p>
     *
     * @param name         The permission node to register (e.g., "grim.exempt").
     * @param defaultValue The default value for the permission, using the cross-platform
     *                     {@link PermissionDefaultValue} enum.
     */
    void registerPermission(String name, PermissionDefaultValue defaultValue);
}
