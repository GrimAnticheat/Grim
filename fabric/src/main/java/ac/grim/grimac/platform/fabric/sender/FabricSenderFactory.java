package ac.grim.grimac.platform.fabric.sender;

import ac.grim.grimac.api.command.SenderKind;
import ac.grim.grimac.platform.api.permissions.PermissionDefaultValue;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.api.sender.SenderFactory;
import ac.grim.grimac.platform.fabric.AbstractFabricPlatformServer;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import ac.grim.grimac.platform.fabric.utils.message.IFabricMessageUtil;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.rcon.RconConsoleSource;
import net.minecraft.world.level.BaseCommandBlock;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FabricSenderFactory extends SenderFactory<CommandSourceStack> {

    public static final boolean HAS_PERMISSIONS_API = FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");

    private final Map<String, PermissionDefaultValue> permissionDefaults = new HashMap<>();
    private final AbstractFabricPlatformServer platformServer = GrimACFabricLoaderPlugin.LOADER.getPlatformServer();
    private final IFabricMessageUtil fabricMessageUtils = GrimACFabricLoaderPlugin.LOADER.getFabricMessageUtils();

    @Override
    protected UUID getUniqueId(CommandSourceStack commandSource) {
        if (commandSource.getEntity() != null) {
            return commandSource.getEntity().getUUID();
        }
        return Sender.CONSOLE_UUID;
    }

    @Override
    protected String getName(CommandSourceStack commandSource) {
        String name = commandSource.getTextName();
        if (commandSource.getEntity() != null && name.equals("Server")) {
            return Sender.CONSOLE_NAME;
        }
        return name;
    }

    @Override
    protected void sendMessage(CommandSourceStack sender, String message) {
        fabricMessageUtils.sendMessage(sender, fabricMessageUtils.textLiteral(message), false);
    }

    @Override
    protected void sendMessage(CommandSourceStack sender, Component message) {
        fabricMessageUtils.sendMessage(sender, GrimACFabricLoaderPlugin.LOADER.getFabricConversionUtil().toNativeText(message), false);
    }

    @Override
    protected boolean hasPermission(CommandSourceStack commandSource, String node) {
        TriState permissionValue = TriState.DEFAULT;
        if (HAS_PERMISSIONS_API) {
            permissionValue = Permissions.getPermissionValue(commandSource, node);
            if (permissionValue != TriState.DEFAULT) {
                return permissionValue.get();
            }
        }

        // Check registered default value
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
    protected boolean hasPermission(CommandSourceStack commandSource, String node, boolean defaultIfUnset) {
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
    protected void performCommand(CommandSourceStack sender, String command) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean isConsole(CommandSourceStack sender) {
        CommandSource output = sender.source;
        return output == sender.getServer() || // Console
                output.getClass() == RconConsoleSource.class || // Rcon
                (output == CommandSource.NULL && sender.getTextName().isEmpty()); // Functions
    }

    @Override
    protected boolean isPlayer(CommandSourceStack sender) {
        return sender.getEntity() instanceof ServerPlayer;
    }

    /**
     * Cross-platform categorization. Every symbol referenced here is
     * verified intermediary-stable across MC 1.16.1 → latest via the workflow
     * documented in {@code /opt/grim-setup/docs/FABRIC.md}:
     * <ul>
     *   <li>{@code CommandSourceStack#getEntity()} — already in production parent code.</li>
     *   <li>{@code ServerPlayer} — already in production parent code.</li>
     *   <li>{@code CommandSourceStack.source} (field) — already in production parent code.</li>
     *   <li>{@code CommandSourceStack#getServer()} — already in production parent code.</li>
     *   <li>{@code RconConsoleSource} — already in production parent code.</li>
     *   <li>{@code CommandSource.NULL} — already in production parent code.</li>
     *   <li>{@code CommandSourceStack#getTextName()} — already in production parent code.</li>
     *   <li>{@code BaseCommandBlock} ({@code class_1918}) — verified stable
     *       intermediary across all 35 release versions in range.</li>
     * </ul>
     */
    @Override
    protected @NotNull SenderKind getKind(CommandSourceStack sender) {
        if (sender.getEntity() instanceof ServerPlayer) return SenderKind.PLAYER;
        if (sender.getEntity() != null) return SenderKind.NON_PLAYER_ENTITY;
        CommandSource output = sender.source;
        if (output == sender.getServer()) return SenderKind.CONSOLE;
        if (output.getClass() == RconConsoleSource.class) return SenderKind.REMOTE_CONSOLE;
        if (output == CommandSource.NULL && sender.getTextName().isEmpty()) return SenderKind.FUNCTION;
        if (output instanceof BaseCommandBlock) return SenderKind.COMMAND_BLOCK;
        return SenderKind.OTHER;
    }

    public void registerPermissionDefault(String permission, PermissionDefaultValue defaultValue) {
        permissionDefaults.put(permission, defaultValue);
    }
}
