package ac.grim.grimac.platform.fabric.sender;

import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.api.sender.SenderFactory;
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

import java.util.UUID;

// fabric-official SenderFactory. Uses fabric-permissions-api (official mappings on 26.1)
// for permission checks when the mod is installed, mirroring fabric-intermediary's
// FabricSenderFactory, and falls back to the vanilla op level otherwise. (Renamed from
// NoopFabricSenderFactory: it was never a no-op — it does real op-level / permission checks.)
public class FabricSenderFactory extends SenderFactory<CommandSourceStack> {

    // fabric-permissions-api is an optional soft dependency; when its mod is absent
    // we fall back to the vanilla op-level check (hasCommandLevel) below.
    private static final boolean HAS_PERMISSIONS_API =
            FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");

    @Override
    protected UUID getUniqueId(CommandSourceStack source) {
        if (source.getEntity() != null) {
            return source.getEntity().getUUID();
        }
        return Sender.CONSOLE_UUID;
    }

    @Override
    protected String getName(CommandSourceStack source) {
        String name = source.getTextName();
        if (source.getEntity() != null && name.equals("Server")) {
            return Sender.CONSOLE_NAME;
        }
        return name;
    }

    @Override
    protected void sendMessage(CommandSourceStack source, String message) {
        System.out.println(message);
    }

    @Override
    protected void sendMessage(CommandSourceStack source, Component message) {
        // Flatten via ComponentFlattener (no formatting, but text-only is enough for now;
        // adventure-platform-fabric isn't available for 26.X).
        StringBuilder out = new StringBuilder();
        ComponentFlattener.basic().flatten(message, out::append);
        sendMessage(source, out.toString());
    }

    @Override
    protected boolean hasPermission(CommandSourceStack source, String node) {
        if (HAS_PERMISSIONS_API) {
            // 0.7.0: getPermissionValue returns fabric-api's TriState; only defer to
            // the op-level fallback when the node is unset (TriState.DEFAULT).
            TriState permissionValue = Permissions.getPermissionValue(source, node);
            if (permissionValue != TriState.DEFAULT) {
                return permissionValue.get();
            }
        }
        return hasCommandLevel(source);
    }

    @Override
    protected boolean hasPermission(CommandSourceStack source, String node, boolean defaultIfUnset) {
        if (HAS_PERMISSIONS_API) {
            // 0.7.0: Permissions.check honors defaultIfUnset when the node is unset.
            return Permissions.check(source, node, defaultIfUnset);
        }
        return defaultIfUnset || hasCommandLevel(source);
    }

    // Vanilla op-level fallback used when fabric-permissions-api is not installed.
    // 26.X: hasPermission(int) → permissions().hasPermission(Permission).
    private boolean hasCommandLevel(CommandSourceStack source) {
        return source.permissions().hasPermission(
                new Permission.HasCommandLevel(PermissionLevel.byId(2)));
    }

    @Override
    protected void performCommand(CommandSourceStack source, String command) {
        throw new UnsupportedOperationException("performCommand not implemented on 26.X scaffold");
    }

    @Override
    protected boolean isConsole(CommandSourceStack source) {
        CommandSource out = source.source;
        return out == source.getServer()
                || out.getClass() == RconConsoleSource.class
                || (out == CommandSource.NULL && source.getTextName().isEmpty());
    }

    @Override
    protected boolean isPlayer(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer;
    }
}
