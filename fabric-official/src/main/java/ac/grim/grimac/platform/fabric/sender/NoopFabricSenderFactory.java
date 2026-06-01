package ac.grim.grimac.platform.fabric.sender;

import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.api.sender.SenderFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.rcon.RconConsoleSource;

import java.util.UUID;

// fabric-official SenderFactory. Avoids fabric-permissions-api (intermediary-bound)
// and falls back to vanilla op level for permission checks.
public class NoopFabricSenderFactory extends SenderFactory<CommandSourceStack> {

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
        // Flatten via ComponentFlattener (no formatting, but text-only is enough for now
        // — adventure-platform-fabric isn't available for 26.X).
        StringBuilder out = new StringBuilder();
        ComponentFlattener.basic().flatten(message, out::append);
        sendMessage(source, out.toString());
    }

    @Override
    protected boolean hasPermission(CommandSourceStack source, String node) {
        // 26.X: hasPermission(int) → permissions().hasPermission(Permission).
        // Fall back to op level 2 since fabric-permissions-api isn't ported.
        return source.permissions().hasPermission(
                new Permission.HasCommandLevel(PermissionLevel.byId(2)));
    }

    @Override
    protected boolean hasPermission(CommandSourceStack source, String node, boolean defaultIfUnset) {
        return defaultIfUnset ? true : hasPermission(source, node);
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
