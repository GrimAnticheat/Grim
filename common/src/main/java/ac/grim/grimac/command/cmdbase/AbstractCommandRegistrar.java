package ac.grim.grimac.command.cmdbase;

import ac.grim.grimac.api.command.AbstractCommand;
import ac.grim.grimac.command.CloudCommandService;
import ac.grim.grimac.command.SenderRequirement;
import ac.grim.grimac.command.requirements.PlayerSenderRequirement;
import ac.grim.grimac.platform.api.sender.Sender;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.CloudCapability;
import org.incendo.cloud.parser.standard.StringArrayParser;
import org.incendo.cloud.processors.requirements.Requirements;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Walks an {@link AbstractCommand} tree and emits Cloud command registrations.
 * Each node — group or leaf — registers as an executable Cloud command at its
 * literal path, with an optional {@code String[] args} tail captured by
 * {@link StringArrayParser}. Permissions resolve to a single
 * {@link PermissionSenderRequirement} per node (OR semantics, with
 * {@code grim.dev} as the implicit alternative); {@code requiresPlayer} adds a
 * {@link PlayerSenderRequirement}.
 *
 * <p>Returns a {@link Registration} handle from each register call; cleanup
 * either deletes the root command (for top-level mounts) or no-ops (for
 * {@code /grim}-spliced subtrees, where rebuilding the parent tree is the
 * caller's responsibility — we don't currently support partial subtree
 * removal).
 */
public final class AbstractCommandRegistrar {

    private static final Pattern PERMISSION_PATTERN = Pattern.compile("^[a-z][a-z_0-9]*\\.command(\\.[a-z_0-9]+)+$");

    public interface Registration {
        /** Removes this registration from the Cloud manager, if supported. */
        void unregister();
    }

    private final CommandManager<Sender> manager;

    public AbstractCommandRegistrar(@NotNull CommandManager<Sender> manager) {
        this.manager = manager;
    }

    /**
     * Registers a top-level command tree. The root's {@link AbstractCommand#getName()}
     * (and aliases) become root-level Minecraft commands. The permission namespace
     * is derived from the root's name — e.g., a root named {@code myext} produces
     * permissions like {@code myext.command.<dotted.path>} for nodes that use
     * {@link AbstractCommand#suggestPerm()}.
     */
    public @NotNull Registration register(@NotNull AbstractCommand root) {
        return register(root, root.getName());
    }

    /**
     * Registers a top-level command tree at the given Cloud root name(s). Used
     * by {@code CommandRegistryImpl.registerUnderGrim} to splice extension
     * commands beneath the {@code /grim} root — pass the parent's full alias
     * list as the Cloud root, then walk the new subtree with {@code root} as
     * the first literal under it.
     */
    public @NotNull Registration registerWithRoot(@NotNull AbstractCommand root, @NotNull String permissionNamespace,
                                                  @NotNull String... cloudRootAliases) {
        if (cloudRootAliases.length == 0) {
            throw new IllegalArgumentException("Cloud root aliases must not be empty");
        }
        String[] aliases = Arrays.copyOfRange(cloudRootAliases, 1, cloudRootAliases.length);
        Command.Builder<Sender> builder = manager.commandBuilder(cloudRootAliases[0], aliases);
        Set<String> rootCommandNames = new HashSet<>();
        rootCommandNames.add(cloudRootAliases[0]);
        rootCommandNames.addAll(Arrays.asList(aliases));
        walk(root, builder, permissionNamespace);
        return new RootRegistration(rootCommandNames);
    }

    private @NotNull Registration register(@NotNull AbstractCommand root, @NotNull String permissionNamespace) {
        return registerWithRoot(root, permissionNamespace, prependName(root));
    }

    private static String[] prependName(AbstractCommand root) {
        List<String> names = new ArrayList<>();
        names.add(root.getName());
        names.addAll(root.getAliases());
        return names.toArray(new String[0]);
    }

    private void walk(@NotNull AbstractCommand node, @NotNull Command.Builder<Sender> builderAtNode,
                      @NotNull String permissionNamespace) {
        // Every node — group or leaf — registers as executable. Group commands
        // inherit AbstractCommand.onCommand's default which prints usage,
        // matching the legacy 3.0 behavior. The `hidden` flag is currently
        // ignored at registration; tab-complete hiding is provided by the
        // requirement postprocessor's hide-on-fail when permissions don't
        // grant access.
        registerExecutable(node, builderAtNode, permissionNamespace);

        Set<AbstractCommand> seen = new HashSet<>();
        for (AbstractCommand child : node.getChildren().values()) {
            if (!seen.add(child)) continue;
            String[] childAliases = child.getAliases().toArray(new String[0]);
            Command.Builder<Sender> childBuilder = builderAtNode.literal(child.getName(), childAliases);
            walk(child, childBuilder, permissionNamespace);
        }
    }

    private void registerExecutable(@NotNull AbstractCommand node,
                                    @NotNull Command.Builder<Sender> builderAtNode,
                                    @NotNull String permissionNamespace) {
        BlockingSuggestionProvider.Strings<Sender> argSuggestions = (ctx, in) -> {
            // Pass the remaining tokens to onTabComplete as a String[] in the
            // legacy 3.0 shape. Cloud's CommandInput exposes peekString() for
            // the unconsumed tail.
            String remaining = in.peekString();
            String[] args = remaining.isEmpty()
                    ? new String[0]
                    : remaining.split(" ", -1);
            return node.onTabComplete(node, ctx.sender(), node.fullPath(), args);
        };

        Command.Builder<Sender> leaf = builderAtNode
                .optional("args", StringArrayParser.stringArrayParser(), argSuggestions)
                .handler(ctx -> {
                    String[] args = ctx.<String[]>optional("args").orElse(new String[0]);
                    String label = node.fullPath();
                    node.onCommand(ctx.sender(), label, args);
                });

        List<SenderRequirement> requirements = buildRequirements(node, permissionNamespace);
        if (!requirements.isEmpty()) {
            leaf = leaf.apply(CloudCommandService.REQUIREMENT_FACTORY.create(Requirements.of(requirements)));
        }
        manager.command(leaf);
    }

    private static List<SenderRequirement> buildRequirements(AbstractCommand node, String permissionNamespace) {
        List<SenderRequirement> out = new ArrayList<>();
        if (node.isRequiresPlayer()) {
            out.add(PlayerSenderRequirement.PLAYER_SENDER_REQUIREMENT);
        }

        List<String> resolvedPermissions = new ArrayList<>();
        boolean sawDev = false;
        boolean sawSuggested = false;

        for (String raw : node.getPermissions()) {
            if (raw == null || raw.isBlank()) continue;
            if (DevSenderRequirement.DEV_PERMISSION.equals(raw)) {
                sawDev = true;
                continue;
            }
            if (isSuggestPlaceholder(raw)) {
                if (!sawSuggested) {
                    String derived = derivePermission(node, permissionNamespace);
                    if (!PERMISSION_PATTERN.matcher(derived).matches()) {
                        throw new IllegalStateException("Invalid derived permission: " + derived
                                + " for command " + node.fullPath());
                    }
                    resolvedPermissions.add(derived);
                    sawSuggested = true;
                }
                continue;
            }
            resolvedPermissions.add(raw);
        }

        // Dev-only command (only grim.dev, no other permissions): single
        // requirement that *only* grants on dev — hide-on-fail kicks in
        // for non-developers.
        if (resolvedPermissions.isEmpty() && sawDev) {
            out.add(DevSenderRequirement.INSTANCE);
            return out;
        }

        if (!resolvedPermissions.isEmpty()) {
            // grim.dev is implicitly OR'd in via PermissionSenderRequirement's
            // devAlternative=true. Setting devAlternative=false would only
            // matter if the caller explicitly wanted to exclude dev fallback.
            out.add(new PermissionSenderRequirement(resolvedPermissions, true));
        }
        return out;
    }

    private static String derivePermission(AbstractCommand node, String namespace) {
        String path = node.derivedPath();
        return path.isEmpty()
                ? namespace + ".command"
                : namespace + ".command." + path;
    }

    private static boolean isSuggestPlaceholder(String s) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) != '?') return false;
        }
        return true;
    }

    private final class RootRegistration implements Registration {
        private final Set<String> rootNames;
        private boolean cleared = false;

        RootRegistration(Set<String> rootNames) {
            this.rootNames = rootNames;
        }

        @Override
        public void unregister() {
            if (cleared) return;
            cleared = true;
            for (String name : rootNames) {
                try {
                    manager.deleteRootCommand(name);
                } catch (CloudCapability.CloudCapabilityMissingException ignored) {
                    // Platform doesn't support deletion; skip. Plugin-disable on
                    // platforms without this capability simply leaves the
                    // commands registered for the rest of the server lifetime.
                }
            }
        }
    }
}
