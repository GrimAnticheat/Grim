package ac.grim.grimac.platform.api.sender;

import ac.grim.grimac.api.command.CommandSender;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Wrapper interface to represent a CommandSender/CommandSource within the common command implementations.
 *
 * <p>Extends the api-public {@link CommandSender} surface — extension code only
 * sees that, while the common runtime works with the richer {@code Sender}
 * (adds {@link #sendMessage(Component)}, {@link #getNativeSender()},
 * {@link #getPlatformPlayer()}, and the {@code defaultIfUnset} permission overload).
 */
public interface Sender extends CommandSender {

    /**
     * Send a component message to the Sender.
     *
     * @param message the component message to send.
     */
    void sendMessage(Component message);

    /**
     * Check if the Sender has a permission.
     *
     * @param permission     the permission to check for
     * @param defaultIfUnset the default value of the permission, if not yet set.
     * @return true if the sender has the permission
     */
    boolean hasPermission(String permission, boolean defaultIfUnset);

    /**
     * Gets the native platform-specific command sender object.
     *
     * @return The platform's native command sender type:
     * <ul>
     * <li>Bukkit/Spigot/Paper/Folia/Pufferfish/etc... {@code org.bukkit.command.CommandSender}</li>
     * <li>Fabric:
     *     <ul>
     *     <li>Yarn: {@code net.minecraft.server.command.ServerCommandSource}</li>
     *     <li>Mojmap: {@code net.minecraft.commands.CommandSourceStack}</li>
     *     </ul>
     * <li>Velocity: {@code com.velocitypowered.api.command.CommandSource}</li>
     * <li>BungeeCord: {@code net.md_5.bungee.api.CommandSender}</li>
     * <li>Sponge: {@code org.spongepowered.api.command.CommandCause}</li>
     * <li>Forge/NeoForge: {@code net.minecraft.commands.CommandSourceStack}</li>
     * </ul>
     */
    @NotNull Object getNativeSender();

    /**
     * Gets the PlatformPlayer tied to a sender
     *
     * @return PlatformPlayer wrapping the underlying native platform-specific player type, null if Sender is not a player
     */
    @Nullable PlatformPlayer getPlatformPlayer();
}
