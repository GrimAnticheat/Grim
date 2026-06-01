package ac.grim.grimac.platform.fabric.sender;

import ac.grim.grimac.platform.api.permissions.PermissionDefaultValue;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.api.sender.SenderFactory;
import ac.grim.grimac.platform.fabric.AbstractFabricPlatformServer;
import ac.grim.grimac.platform.fabric.GrimACFabricIntermediaryLoaderPlugin;
import ac.grim.grimac.platform.fabric.utils.message.IFabricMessageUtil;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.rcon.RconConsoleSource;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class FabricIntermediarySenderFactory extends SenderFactory<CommandSourceStack> {

    public static final boolean HAS_PERMISSIONS_API = FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");

    private final Map<String, PermissionDefaultValue> permissionDefaults = new HashMap<>();
    private final AbstractFabricPlatformServer platformServer = GrimACFabricIntermediaryLoaderPlugin.LOADER.getPlatformServer();
    private final IFabricMessageUtil fabricMessageUtils = GrimACFabricIntermediaryLoaderPlugin.LOADER.getFabricMessageUtils();

    @Override
    public @NotNull Sender wrap(@NotNull CommandSourceStack sender) {
        return Objects.requireNonNull(sender, "sender");
    }

    @Override
    public UUID getUniqueId(CommandSourceStack commandSource) {
        if (commandSource.getEntity() != null) {
            return commandSource.getEntity().getUUID();
        }
        return Sender.CONSOLE_UUID;
    }

    @Override
    public String getName(CommandSourceStack commandSource) {
        String name = commandSource.getTextName();
        if (commandSource.getEntity() != null && name.equals("Server")) {
            return Sender.CONSOLE_NAME;
        }
        return name;
    }

    @Override
    public void sendMessage(CommandSourceStack sender, String message) {
        fabricMessageUtils.sendMessage((Sender) (Object) sender, fabricMessageUtils.textLiteral(message), false);
    }

    @Override
    public void sendMessage(CommandSourceStack sender, Component message) {
        net.minecraft.network.chat.Component nativeText =
                (net.minecraft.network.chat.Component) GrimACFabricIntermediaryLoaderPlugin.LOADER.getFabricConversionUtil().toNativeText(message);
        fabricMessageUtils.sendMessage((Sender) (Object) sender, nativeText, false);
    }

    @Override
    public boolean hasPermission(CommandSourceStack commandSource, String node) {
        TriState permissionValue = TriState.DEFAULT;
        if (HAS_PERMISSIONS_API) {
            permissionValue = Permissions.getPermissionValue(commandSource, node);
            if (permissionValue != TriState.DEFAULT) {
                return permissionValue.get();
            }
        }

        PermissionDefaultValue defaultValue = permissionDefaults.get(node);
        if (defaultValue == null) {
            return platformServer.hasPermission(commandSource, platformServer.getOperatorPermissionLevel());
        }

        return switch (defaultValue) {
            case TRUE -> true;
            case FALSE -> false;
            case OP -> platformServer.hasPermission(commandSource, platformServer.getOperatorPermissionLevel());
            case NOT_OP -> !platformServer.hasPermission(commandSource, platformServer.getOperatorPermissionLevel());
        };
    }

    @Override
    public boolean hasPermission(CommandSourceStack commandSource, String node, boolean defaultIfUnset) {
        if (HAS_PERMISSIONS_API)
            return Permissions.check(commandSource, node, defaultIfUnset);
        else {
            PermissionDefaultValue defaultValue = permissionDefaults.get(node);
            if (defaultValue == null) {
                return defaultIfUnset;
            }

            return switch (defaultValue) {
                case TRUE -> true;
                case FALSE -> false;
                case OP -> platformServer.hasPermission(commandSource, platformServer.getOperatorPermissionLevel());
                case NOT_OP -> !platformServer.hasPermission(commandSource, platformServer.getOperatorPermissionLevel());
            };
        }
    }

    @Override
    public void performCommand(CommandSourceStack sender, String command) {
        platformServer.dispatchCommand((Sender) (Object) sender, command);
    }

    @Override
    public boolean isConsole(CommandSourceStack sender) {
        CommandSource output = sender.source;
        return output == sender.getServer()
                || output.getClass() == RconConsoleSource.class
                || (output == CommandSource.NULL && sender.getTextName().isEmpty());
    }

    @Override
    public boolean isPlayer(CommandSourceStack sender) {
        return sender.getEntity() instanceof ServerPlayer;
    }

    public void registerPermissionDefault(String permission, PermissionDefaultValue defaultValue) {
        permissionDefaults.put(permission, defaultValue);
    }
}
