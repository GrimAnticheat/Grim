package ac.grim.grimac.command.cmdbase;

import ac.grim.grimac.api.command.AbstractCommand;
import ac.grim.grimac.api.command.CommandRegistry;
import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.platform.api.sender.Sender;
import org.incendo.cloud.CommandManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default {@link CommandRegistry} implementation. Tracks registrations by
 * owning {@link GrimPlugin} so plugin-disable cleanup can sweep them.
 *
 * <p>Top-level mounts use {@link AbstractCommandRegistrar#register(AbstractCommand)}
 * and clean up via Cloud's {@code deleteRootCommand} when the owner disables.
 *
 * <p>{@code registerUnderGrim} is currently unsupported — splicing into the
 * built-in {@code /grim} root requires per-subtree unregister support that
 * Cloud doesn't provide directly. Tracked as a follow-up; for now extensions
 * mount their own top-level command.
 */
public final class CommandRegistryImpl implements CommandRegistry {

    private final AbstractCommandRegistrar registrar;
    private final Map<GrimPlugin, List<Entry>> byOwner = new ConcurrentHashMap<>();
    private final Map<AbstractCommand, Entry> byCommand = new IdentityHashMap<>();

    public CommandRegistryImpl(@NotNull CommandManager<Sender> manager) {
        this.registrar = new AbstractCommandRegistrar(manager);
    }

    @Override
    public synchronized void register(@NotNull GrimPlugin owner, @NotNull AbstractCommand command) {
        AbstractCommandRegistrar.Registration reg = registrar.register(command);
        Entry entry = new Entry(owner, command, reg);
        byOwner.computeIfAbsent(owner, k -> new ArrayList<>()).add(entry);
        byCommand.put(command, entry);
    }

    @Override
    public void registerUnderGrim(@NotNull GrimPlugin owner, @NotNull AbstractCommand command) {
        throw new UnsupportedOperationException("registerUnderGrim is not yet supported. Mount as a top-level command via register() instead.");
    }

    @Override
    public synchronized void unregister(@NotNull AbstractCommand command) {
        Entry entry = byCommand.remove(command);
        if (entry == null) return;
        entry.registration.unregister();
        List<Entry> entries = byOwner.get(entry.owner);
        if (entries != null) entries.remove(entry);
    }

    @Override
    public synchronized void unregisterAll(@NotNull GrimPlugin owner) {
        List<Entry> entries = byOwner.remove(owner);
        if (entries == null) return;
        for (Entry entry : entries) {
            byCommand.remove(entry.command);
            entry.registration.unregister();
        }
    }

    private record Entry(GrimPlugin owner, AbstractCommand command,
                         AbstractCommandRegistrar.Registration registration) {
    }
}
