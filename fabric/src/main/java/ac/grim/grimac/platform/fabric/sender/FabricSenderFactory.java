package ac.grim.grimac.platform.fabric.sender;

import ac.grim.grimac.platform.api.permissions.PermissionDefaultValue;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.api.sender.SenderFactory;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import ac.grim.grimac.platform.fabric.utils.message.IFabricMessageUtil;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.util.TriState;
import net.kyori.adventure.text.Component;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.rcon.RconCommandOutput;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.SenderMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FabricSenderFactory extends SenderFactory<ServerCommandSource> implements SenderMapper<ServerCommandSource, Sender> {

    private final Map<String, PermissionDefaultValue> permissionDefaults = new HashMap<>();
    private static final IFabricMessageUtil fabricMessageUtils = GrimACFabricLoaderPlugin.LOADER.getFabricMessageUtils();

    @Override
    protected UUID getUniqueId(ServerCommandSource commandSource) {
        if (commandSource.getEntity() != null) {
            return commandSource.getEntity().getUuid();
        }
        return Sender.CONSOLE_UUID;
    }

    @Override
    protected String getName(ServerCommandSource commandSource) {
        String name = commandSource.getName();
        if (commandSource.getEntity() != null && name.equals("Server")) {
            return Sender.CONSOLE_NAME;
        }
        return name;
    }

    @Override
    protected void sendMessage(ServerCommandSource sender, String message) {
        fabricMessageUtils.sendMessage(sender, fabricMessageUtils.textLiteral(message), false);
    }

    @Override
    protected void sendMessage(ServerCommandSource sender, Component message) {
        fabricMessageUtils.sendMessage(sender, GrimACFabricLoaderPlugin.LOADER.getFabricConversionUtil().toNativeText(message), false);
    }

    @Override
    protected boolean hasPermission(ServerCommandSource commandSource, String node) {
        TriState permissionValue = Permissions.getPermissionValue(commandSource, node);
        if (permissionValue != TriState.DEFAULT) {
            return permissionValue.get();
        }

        // Check registered default value
        PermissionDefaultValue defaultValue = permissionDefaults.get(node);
        if (defaultValue == null) {
            return permissionValue.get(); // Fallback to provided default if unset
        }

        return switch (defaultValue) {
            case TRUE -> true;
            case FALSE -> false;
            case OP -> commandSource.hasPermissionLevel(GrimACFabricLoaderPlugin.FABRIC_SERVER.getOpPermissionLevel());
            case NOT_OP -> !commandSource.hasPermissionLevel(GrimACFabricLoaderPlugin.FABRIC_SERVER.getOpPermissionLevel());
        };
    }

    @Override
    protected boolean hasPermission(ServerCommandSource commandSource, String node, boolean defaultIfUnset) {
        return Permissions.check(commandSource, node, defaultIfUnset);
    }

    @Override
    protected void performCommand(ServerCommandSource sender, String command) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean isConsole(ServerCommandSource sender) {
        CommandOutput output = sender.output;
        return output == sender.getMinecraftServer() || // Console
                output.getClass() == RconCommandOutput.class || // Rcon
                (output == CommandOutput.DUMMY && sender.getName().isEmpty()); // Functions
    }

    @Override
    protected boolean isPlayer(ServerCommandSource sender) {
        return sender.getEntity() instanceof ServerPlayerEntity;
    }

    @Override
    public @NonNull Sender map(@NonNull ServerCommandSource base) {
        return this.wrap(base);
    }

    @Override
    public @NonNull ServerCommandSource reverse(@NonNull Sender mapped) {
        return this.unwrap(mapped);
    }

    public void registerPermissionDefault(String permission, PermissionDefaultValue defaultValue) {
        permissionDefaults.put(permission, defaultValue);
    }
}
