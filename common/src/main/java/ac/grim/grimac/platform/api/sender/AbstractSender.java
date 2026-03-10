package ac.grim.grimac.platform.api.sender;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Simple implementation of {@link Sender} using a {@link SenderFactory}
 *
 * @param <T> the command sender type
 */
public final class AbstractSender<T> implements Sender {
    private final SenderFactory<T> factory;
    private final T sender;

    private final UUID uniqueId;
    private final String name;
    private final boolean isConsole;

    AbstractSender(@NotNull SenderFactory<T> factory, @NotNull T sender) {
        this.factory = factory;
        this.sender = sender;
        this.uniqueId = factory.getUniqueId(this.sender);
        this.name = factory.getName(this.sender);
        this.isConsole = factory.isConsole(this.sender);
    }

    @Override
    public UUID getUniqueId() {
        return this.uniqueId;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void sendMessage(String message) {
        this.factory.sendMessage(this.sender, message);
    }

    @Override
    public void sendMessage(Component message) {
        this.factory.sendMessage(this.sender, message);
    }

    @Override
    public boolean hasPermission(String permission) {
        return isConsole() || this.factory.hasPermission(this.sender, permission);
    }

    @Override
    public boolean hasPermission(String permission, boolean defaultIfUnset) {
        return isConsole() || this.factory.hasPermission(this.sender, permission, defaultIfUnset);
    }

    @Override
    public void performCommand(String commandLine) {
        this.factory.performCommand(this.sender, commandLine);
    }

    @Override
    public boolean isConsole() {
        return this.isConsole;
    }

    @Override
    public boolean isPlayer() {
        return this.factory.isPlayer(sender);
    }

    @Override
    public boolean isValid() {
        return true;
//        return isConsole() || this.plugin.getBootstrap().isPlayerOnline(this.uniqueId);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof AbstractSender<?> that)) return false;
        return this.getUniqueId().equals(that.getUniqueId());
    }

    @Override
    public int hashCode() {
        return this.uniqueId.hashCode();
    }

    @Override
    public @NotNull T getNativeSender() {
        return sender;
    }

    @Override
    public @Nullable PlatformPlayer getPlatformPlayer() {
        return GrimAPI.INSTANCE.getPlatformPlayerFactory().getFromUUID(this.getUniqueId());
    }
}
