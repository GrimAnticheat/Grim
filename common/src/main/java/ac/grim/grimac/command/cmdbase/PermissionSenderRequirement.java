package ac.grim.grimac.command.cmdbase;

import ac.grim.grimac.command.SenderRequirement;
import ac.grim.grimac.platform.api.sender.Sender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Requirement that grants access if the sender has any of a list of permissions.
 * The OR semantics are baked into a single requirement because Cloud's
 * {@link org.incendo.cloud.processors.requirements.RequirementPostprocessor}
 * runs requirements in AND mode — a list of separate requirements would all
 * have to pass.
 *
 * <p>{@code grim.dev} is treated as an implicit always-OR alternative when
 * {@code devAlternative} is true (the default for hierarchy-mounted commands).
 */
public final class PermissionSenderRequirement implements SenderRequirement {

    private final List<String> permissions;
    private final boolean devAlternative;

    public PermissionSenderRequirement(@NotNull List<String> permissions, boolean devAlternative) {
        this.permissions = List.copyOf(permissions);
        this.devAlternative = devAlternative;
    }

    public PermissionSenderRequirement(@NotNull String permission) {
        this(List.of(permission), true);
    }

    public @NotNull List<String> permissions() {
        return permissions;
    }

    public boolean devAlternative() {
        return devAlternative;
    }

    @Override
    public boolean evaluateRequirement(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (sender.isConsole()) return true;
        if (devAlternative && sender.hasPermission(DevSenderRequirement.DEV_PERMISSION)) return true;
        for (String p : permissions) {
            if (sender.hasPermission(p)) return true;
        }
        return false;
    }

    @Override
    public @NotNull Component errorMessage(Sender sender) {
        if (permissions.size() == 1) {
            return Component.text("You lack permission: " + permissions.get(0), NamedTextColor.RED);
        }
        return Component.text("You lack any of: " + String.join(", ", permissions), NamedTextColor.RED);
    }
}
