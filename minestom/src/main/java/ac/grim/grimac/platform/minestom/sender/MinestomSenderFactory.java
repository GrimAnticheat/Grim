package ac.grim.grimac.platform.minestom.sender;

import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.api.sender.SenderFactory;
import ac.grim.grimac.platform.minestom.MinestomGrimBridge;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;

import java.util.UUID;

/**
 * Grim {@link SenderFactory} over Minestom's {@link CommandSender} (players + console).
 * <p>
 * {@link #hasPermission} routes player checks through {@link MinestomGrimBridge} (the monorepo's
 * custom group system, installed before boot); the console always passes. Until the bridge is
 * installed it denies all player perms — the safe default (no silent bypass).
 */
public final class MinestomSenderFactory extends SenderFactory<CommandSender> {

    @Override
    protected UUID getUniqueId(CommandSender sender) {
        return sender instanceof Player player ? player.getUuid() : Sender.CONSOLE_UUID;
    }

    @Override
    protected String getName(CommandSender sender) {
        return sender instanceof Player player ? player.getUsername() : "CONSOLE";
    }

    @Override
    protected void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(message);
    }

    @Override
    protected void sendMessage(CommandSender sender, Component message) {
        sender.sendMessage(message);
    }

    @Override
    protected boolean hasPermission(CommandSender sender, String node) {
        if (sender instanceof Player player) {
            return MinestomGrimBridge.hasPermission(player, node);
        }
        return isConsole(sender); // console has all perms
    }

    @Override
    protected boolean hasPermission(CommandSender sender, String node, boolean defaultIfUnset) {
        if (sender instanceof Player player) {
            return MinestomGrimBridge.hasPermission(player, node) || defaultIfUnset;
        }
        return isConsole(sender) || defaultIfUnset;
    }

    @Override
    protected void performCommand(CommandSender sender, String command) {
        MinecraftServer.getCommandManager().execute(sender, command);
    }

    @Override
    protected boolean isConsole(CommandSender sender) {
        return !(sender instanceof Player);
    }

    @Override
    protected boolean isPlayer(CommandSender sender) {
        return sender instanceof Player;
    }
}
