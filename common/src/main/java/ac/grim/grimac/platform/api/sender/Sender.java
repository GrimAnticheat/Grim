package ac.grim.grimac.platform.api.sender;

import ac.grim.grimac.platform.api.player.PlatformPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Wrapper interface to represent a CommandSender/CommandSource within the common command implementations.
 */
public interface Sender {

    /**
     * The uuid used by the console sender.
     */
    UUID CONSOLE_UUID = new UUID(0, 0); // 00000000-0000-0000-0000-000000000000

    /**
     * The name used by the console sender.
     */
    String CONSOLE_NAME = "Console";

    /**
     * Gets the sender's username
     *
     * @return a friendly username for the sender
     */
    String getName();

    /**
     * Gets the sender's unique id.
     *
     * <p>See {@link #CONSOLE_UUID} for the console's UUID representation.</p>
     *
     * @return the sender's uuid
     */
    UUID getUniqueId();

    /**
     * Send a json message to the Sender.
     *
     * @param message the message to send.
     */
    void sendMessage(String message);

    /**
     * Send a component message to the Sender.
     *
     * @param message the component message to send.
     */
    void sendMessage(Component message);

    /**
     * Check if the Sender has a permission.
     *
     * @param permission the permission to check for
     * @return true if the sender has the permission
     */
    boolean hasPermission(String permission);

    /**
     * Check if the Sender has a permission.
     *
     * @param permission     the permission to check for
     * @param defaultIfUnset the default value of the permission, if not yet set.
     * @return true if the sender has the permission
     */
    boolean hasPermission(String permission, boolean defaultIfUnset);

    /**
     * Makes the sender perform a command.
     *
     * @param commandLine the command
     */
    void performCommand(String commandLine);

    /**
     * Gets whether this sender is the console
     *
     * @return if the sender is the console
     */
    boolean isConsole();

    /**
     * Gets whether this sender is a player
     *
     * @return if the sender is a player
     */
    boolean isPlayer();

    /**
     * Gets whether this sender is still valid and receiving messages.
     *
     * @return if this sender is valid
     */
    default boolean isValid() {
        return true;
    }

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
