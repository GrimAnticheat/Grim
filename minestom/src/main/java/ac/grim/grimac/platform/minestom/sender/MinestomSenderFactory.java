package ac.grim.grimac.platform.minestom.sender;

import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.api.sender.SenderFactory;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;

import java.util.UUID;

/**
 * Grim {@link SenderFactory} over Minestom's {@link CommandSender} (players + console).
 * <p>
 * TODO Phase 4: {@link #hasPermission} currently grants nothing to players (only the console
 * passes) — the real check must route through the monorepo's custom group system, which
 * {@code grim-minestom} cannot see. Until then Grim admin perms (alerts/bypass/commands) are
 * console-only, which is the safe default.
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
        return isConsole(sender);
    }

    @Override
    protected boolean hasPermission(CommandSender sender, String node, boolean defaultIfUnset) {
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
