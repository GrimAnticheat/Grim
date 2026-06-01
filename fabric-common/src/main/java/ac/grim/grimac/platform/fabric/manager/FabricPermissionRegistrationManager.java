package ac.grim.grimac.platform.fabric.manager;

import ac.grim.grimac.platform.api.manager.PermissionRegistrationManager;
import ac.grim.grimac.platform.api.permissions.PermissionDefaultValue;
import ac.grim.grimac.platform.fabric.sender.AbstractFabricSenderFactory;

import java.util.function.Consumer;

/**
 * Registers Grim's static permission defaults into the shared
 * {@link AbstractFabricSenderFactory} registry, identically for every mapping
 * family. {@code onRegister} is a per-variant hook that primes the node with the
 * permissions-API provider (it touches {@code net.minecraft} command sources, so
 * it cannot live in fabric-common); pass a no-op when there is nothing to prime.
 */
public class FabricPermissionRegistrationManager implements PermissionRegistrationManager {

    private final AbstractFabricSenderFactory<?> senderFactory;
    private final Consumer<String> onRegister;

    public FabricPermissionRegistrationManager(AbstractFabricSenderFactory<?> senderFactory,
                                               Consumer<String> onRegister) {
        this.senderFactory = senderFactory;
        this.onRegister = onRegister;
        registerPermission("grim.exempt", PermissionDefaultValue.FALSE);
        registerPermission("grim.nosetback", PermissionDefaultValue.FALSE);
        registerPermission("grim.nomodifypacket", PermissionDefaultValue.FALSE);
        registerPermission("grim.alerts.enable-on-join", PermissionDefaultValue.FALSE);
        registerPermission("grim.verbose.enable-on-join", PermissionDefaultValue.FALSE);
        registerPermission("grim.brand.enable-on-join", PermissionDefaultValue.FALSE);
        registerPermission("grim.alerts.enable-on-join.silent", PermissionDefaultValue.FALSE);
        registerPermission("grim.verbose.enable-on-join.silent", PermissionDefaultValue.FALSE);
        registerPermission("grim.brand.enable-on-join.silent", PermissionDefaultValue.FALSE);
    }

    @Override
    public void registerPermission(String name, PermissionDefaultValue defaultValue) {
        senderFactory.registerPermissionDefault(name, defaultValue);
        onRegister.accept(name);
    }
}
