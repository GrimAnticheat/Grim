package ac.grim.grimac.platform.fabric.sender;

import ac.grim.grimac.platform.api.permissions.PermissionDefaultValue;
import ac.grim.grimac.platform.api.sender.SenderFactory;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared Fabric sender-factory base. Owns the permission-default registry and the
 * two-tier {@code hasPermission} resolution so both mapping families (intermediary
 * and official) behave identically.
 * <p>
 * Resolution order: a permissions-API value (if a provider is installed and it
 * answers) wins; otherwise the registered {@link PermissionDefaultValue} decides;
 * a node with no registered default falls back to the operator level.
 * <p>
 * NMS-free: the permissions-API query and the operator-level check both reference
 * {@code net.minecraft} types, so they are delegated to abstract hooks implemented
 * per variant. Only the registry and the value resolution live here.
 */
public abstract class AbstractFabricSenderFactory<T> extends SenderFactory<T> {

    public static final boolean HAS_PERMISSIONS_API =
            FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");

    private final Map<String, PermissionDefaultValue> permissionDefaults = new HashMap<>();

    public void registerPermissionDefault(String permission, PermissionDefaultValue defaultValue) {
        permissionDefaults.put(permission, defaultValue);
    }

    /**
     * The permissions-API value for {@code node}, or {@code null} when no provider
     * answers (TriState DEFAULT). Only consulted when {@link #HAS_PERMISSIONS_API}.
     */
    protected abstract @Nullable Boolean queryPermissionValue(T sender, String node);

    /** Permissions-API check honouring the caller's fallback when the node is unset. */
    protected abstract boolean queryPermission(T sender, String node, boolean defaultIfUnset);

    /** Whether this sender meets the configured operator permission level. */
    protected abstract boolean isOperator(T sender);

    @Override
    public boolean hasPermission(T sender, String node) {
        if (HAS_PERMISSIONS_API) {
            Boolean value = queryPermissionValue(sender, node);
            if (value != null) return value;
        }
        PermissionDefaultValue defaultValue = permissionDefaults.get(node);
        if (defaultValue == null) return isOperator(sender);
        return resolve(defaultValue, sender);
    }

    @Override
    public boolean hasPermission(T sender, String node, boolean defaultIfUnset) {
        if (HAS_PERMISSIONS_API) {
            return queryPermission(sender, node, defaultIfUnset);
        }
        PermissionDefaultValue defaultValue = permissionDefaults.get(node);
        if (defaultValue == null) return defaultIfUnset;
        return resolve(defaultValue, sender);
    }

    private boolean resolve(PermissionDefaultValue defaultValue, T sender) {
        return switch (defaultValue) {
            case TRUE -> true;
            case FALSE -> false;
            case OP -> isOperator(sender);
            case NOT_OP -> !isOperator(sender);
        };
    }
}
