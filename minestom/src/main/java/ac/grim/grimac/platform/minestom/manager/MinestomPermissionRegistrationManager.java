package ac.grim.grimac.platform.minestom.manager;

import ac.grim.grimac.platform.api.manager.PermissionRegistrationManager;
import ac.grim.grimac.platform.api.permissions.PermissionDefaultValue;

/**
 * Minestom has no permission registry to register nodes into (permissions are resolved by the
 * monorepo's group system at check time), so registration is a no-op.
 */
public final class MinestomPermissionRegistrationManager implements PermissionRegistrationManager {

    @Override
    public void registerPermission(String name, PermissionDefaultValue defaultValue) {
        // no-op
    }
}
