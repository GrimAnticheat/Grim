package ac.grim.grimac.command.cmdbase;

import ac.grim.grimac.command.SenderRequirement;
import ac.grim.grimac.platform.api.sender.Sender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

/**
 * Gates a command on the {@code grim.dev} permission. Used as the OR-alternative
 * in {@link AbstractCommandRegistrar} so any command's permission set falls
 * through to dev access; also used as the sole requirement on
 * {@link ac.grim.grimac.api.command.AbstractCommand}-marked dev-only nodes.
 */
public final class DevSenderRequirement implements SenderRequirement {

    public static final String DEV_PERMISSION = "grim.dev";
    public static final DevSenderRequirement INSTANCE = new DevSenderRequirement();

    private DevSenderRequirement() {
    }

    @Override
    public boolean evaluateRequirement(@NotNull CommandContext<Sender> context) {
        return context.sender().hasPermission(DEV_PERMISSION);
    }

    @Override
    public @NotNull Component errorMessage(Sender sender) {
        return Component.text("Developer-only command.", NamedTextColor.RED);
    }
}
