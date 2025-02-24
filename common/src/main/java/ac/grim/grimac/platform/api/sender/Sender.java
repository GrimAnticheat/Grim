package ac.grim.grimac.platform.api.sender;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;

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
     * Gets a string representing the senders username, and their current location
     * within the network.
     *
     * @return a friendly identifier for the sender
     */

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
     * Gets whether this sender is still valid & receiving messages.
     *
     * @return if this sender is valid
     */
    default boolean isValid() {
        return true;
    }

    @NonNull Object getSender();
}
