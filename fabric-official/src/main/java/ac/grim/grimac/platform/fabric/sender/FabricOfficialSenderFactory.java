package ac.grim.grimac.platform.fabric.sender;

import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.api.sender.SenderFactory;
import ac.grim.grimac.platform.fabric.AbstractFabricPlatformServer;
import ac.grim.grimac.platform.fabric.GrimACFabricOfficialLoaderPlugin;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.rcon.RconConsoleSource;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public class FabricOfficialSenderFactory extends SenderFactory<CommandSourceStack> {

    private static final boolean HAS_PERMISSIONS_API =
            FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");

    private final AbstractFabricPlatformServer platformServer = GrimACFabricOfficialLoaderPlugin.LOADER.getPlatformServer();

    @Override
    public @NotNull Sender wrap(@NotNull CommandSourceStack source) {
        return Objects.requireNonNull(source, "source");
    }

    @Override
    public UUID getUniqueId(CommandSourceStack source) {
        if (source.getEntity() != null) {
            return source.getEntity().getUUID();
        }
        return Sender.CONSOLE_UUID;
    }

    @Override
    public String getName(CommandSourceStack source) {
        String name = source.getTextName();
        if (source.getEntity() != null && name.equals("Server")) {
            return Sender.CONSOLE_NAME;
        }
        return name;
    }

    @Override
    public void sendMessage(CommandSourceStack source, String message) {
        System.out.println(message);
    }

    @Override
    public void sendMessage(CommandSourceStack source, Component message) {
        StringBuilder out = new StringBuilder();
        ComponentFlattener.basic().flatten(message, out::append);
        sendMessage(source, out.toString());
    }

    @Override
    public boolean hasPermission(CommandSourceStack source, String node) {
        if (HAS_PERMISSIONS_API) {
            TriState permissionValue = Permissions.getPermissionValue(source, node);
            if (permissionValue != TriState.DEFAULT) {
                return permissionValue.get();
            }
        }
        return hasCommandLevel(source);
    }

    @Override
    public boolean hasPermission(CommandSourceStack source, String node, boolean defaultIfUnset) {
        if (HAS_PERMISSIONS_API) {
            return Permissions.check(source, node, defaultIfUnset);
        }
        return defaultIfUnset || hasCommandLevel(source);
    }

    private boolean hasCommandLevel(CommandSourceStack source) {
        return source.permissions().hasPermission(
                new Permission.HasCommandLevel(PermissionLevel.byId(2)));
    }

    @Override
    public void performCommand(CommandSourceStack source, String command) {
        platformServer.dispatchCommand((Sender) (Object) source, command);
    }

    @Override
    public boolean isConsole(CommandSourceStack source) {
        CommandSource out = source.source;
        return out == source.getServer()
                || out.getClass() == RconConsoleSource.class
                || (out == CommandSource.NULL && source.getTextName().isEmpty());
    }

    @Override
    public boolean isPlayer(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer;
    }
}
