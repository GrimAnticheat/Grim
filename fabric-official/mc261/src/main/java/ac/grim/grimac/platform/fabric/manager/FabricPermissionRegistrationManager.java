package ac.grim.grimac.platform.fabric.manager;

import ac.grim.grimac.platform.api.manager.PermissionRegistrationManager;
import ac.grim.grimac.platform.api.permissions.PermissionDefaultValue;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import ac.grim.grimac.platform.fabric.sender.FabricSenderFactory;
import me.lucko.fabric.api.permissions.v0.Permissions;

import static ac.grim.grimac.platform.fabric.sender.FabricSenderFactory.HAS_PERMISSIONS_API;

public class FabricPermissionRegistrationManager implements PermissionRegistrationManager {

    private final FabricSenderFactory fabricSenderFactory = GrimACFabricLoaderPlugin.LOADER.getFabricSenderFactory();

    public FabricPermissionRegistrationManager() {
        registerPermission("grim.alerts", PermissionDefaultValue.OP);
        registerPermission("grim.performance", PermissionDefaultValue.OP);
        registerPermission("grim.profile", PermissionDefaultValue.OP);
        registerPermission("grim.brand", PermissionDefaultValue.OP);
        registerPermission("grim.sendalert", PermissionDefaultValue.OP);
        registerPermission("grim.verbose", PermissionDefaultValue.OP);
        registerPermission("grim.help", PermissionDefaultValue.OP);
        registerPermission("grim.history", PermissionDefaultValue.OP);
        registerPermission("grim.reload", PermissionDefaultValue.OP);
        registerPermission("grim.spectate", PermissionDefaultValue.OP);
        registerPermission("grim.log", PermissionDefaultValue.OP);
        registerPermission("grim.version", PermissionDefaultValue.OP);
        registerPermission("grim.dump", PermissionDefaultValue.OP);
        registerPermission("grim.testwebhook", PermissionDefaultValue.OP);
        registerPermission("grim.debug", PermissionDefaultValue.OP);
        registerPermission("grim.consoledebug", PermissionDefaultValue.OP);

        registerPermission("grim.exempt", PermissionDefaultValue.FALSE);
        registerPermission("grim.nosetback", PermissionDefaultValue.FALSE);
        registerPermission("grim.nomodifypacket", PermissionDefaultValue.FALSE);
        registerPermission("grim.nosetback", PermissionDefaultValue.FALSE);
        registerPermission("grim.alerts.enable-on-join", PermissionDefaultValue.FALSE);
        registerPermission("grim.verbose.enable-on-join", PermissionDefaultValue.FALSE);
        registerPermission("grim.brand.enable-on-join", PermissionDefaultValue.FALSE);
        registerPermission("grim.alerts.enable-on-join.silent", PermissionDefaultValue.FALSE);
        registerPermission("grim.verbose.enable-on-join.silent", PermissionDefaultValue.FALSE);
        registerPermission("grim.brand.enable-on-join.silent", PermissionDefaultValue.FALSE);
        registerPermission("grim.list", PermissionDefaultValue.FALSE);
    }

    @Override
    public void registerPermission(String name, PermissionDefaultValue defaultValue) {
        fabricSenderFactory.registerPermissionDefault(name, defaultValue);
        if (HAS_PERMISSIONS_API)
            Permissions.check(GrimACFabricLoaderPlugin.FABRIC_SERVER.createCommandSourceStack(), name);
    }
}
