package ac.grim.grimac.platform.api.sender;

import ac.grim.grimac.api.command.SenderKind;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Factory class to make a thread-safe sender instance
 *
 * @param <T> the command sender type
 */
public abstract class SenderFactory<T> {
    protected abstract UUID getUniqueId(T sender);

    protected abstract String getName(T sender);

    protected abstract void sendMessage(T sender, String message);

    protected abstract void sendMessage(T sender, Component message);

    protected abstract boolean hasPermission(T sender, String node);

    protected abstract boolean hasPermission(T sender, String node, boolean defaultIfUnset);

    protected abstract void performCommand(T sender, String command);

    protected abstract boolean isConsole(T sender);

    protected abstract boolean isPlayer(T sender);

    /**
     * Categorical view of {@code sender}. Default derives from the existing
     * {@link #isPlayer(Object)} / {@link #isConsole(Object)} checks for
     * back-compat; platform impls should override to disambiguate
     * {@link SenderKind#REMOTE_CONSOLE} vs {@link SenderKind#CONSOLE},
     * detect {@link SenderKind#COMMAND_BLOCK} / {@link SenderKind#FUNCTION},
     * etc.
     */
    protected @NotNull SenderKind getKind(T sender) {
        if (isPlayer(sender)) return SenderKind.PLAYER;
        if (isConsole(sender)) return SenderKind.CONSOLE;
        return SenderKind.OTHER;
    }

    protected boolean shouldSplitNewlines(T sender) {
        return isConsole(sender);
    }

    public final @NotNull Sender wrap(@NotNull T sender) {
        Objects.requireNonNull(sender, "sender");
        return new AbstractSender<>(this, sender);
    }

    @SuppressWarnings("unchecked")
    public final @NotNull T unwrap(@NotNull Sender sender) {
        Objects.requireNonNull(sender, "sender");
        return (T) sender.getNativeSender();
    }
}
