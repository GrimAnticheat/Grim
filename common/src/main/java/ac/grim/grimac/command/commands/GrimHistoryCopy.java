package ac.grim.grimac.command.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.storage.backend.Backend;
import ac.grim.grimac.api.storage.backend.BackendException;
import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.internal.storage.copy.BackendToBackendCopier;
import ac.grim.grimac.manager.datastore.DataStoreLifecycle;
import ac.grim.grimac.platform.api.manager.cloud.CloudCommandAdapter;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;

import java.util.Map;

/**
 * {@code /grim history copy <src-backend-id> <dst-backend-id> [--delete]} —
 * cross-backend copy of sessions + violations + player identities via
 * {@link BackendToBackendCopier}. The {@code --delete} flag wipes the source
 * after the copy completes.
 * <p>
 * Source/destination are resolved by backend id against whatever backends the
 * active routing has wired up. The two built-in backends are {@code sqlite}
 * and {@code memory}; any third-party backend registered via
 * {@link ac.grim.grimac.api.storage.backend.BackendRegistry} plugs in here
 * without changes to this command.
 * <p>
 * Runs synchronously on the command thread so RCON callers see progress and
 * result before the reply channel closes. Sessions dedup via primary key;
 * violations get fresh autoincrement IDs per insert, so running the command
 * twice without {@code --delete} on the destination will duplicate violation
 * rows — see the copier's docstring for details.
 */
public class GrimHistoryCopy implements BuildableCommand {

    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
                commandManager.commandBuilder("grim", "grimac")
                        .literal("history")
                        .literal("copy")
                        .permission("grim.history.copy")
                        .required("src", StringParser.stringParser())
                        .required("dst", StringParser.stringParser())
                        .flag(commandManager.flagBuilder("delete")
                                .withDescription(org.incendo.cloud.description.Description.of(
                                        "Wipe the source backend after the copy completes")))
                        .handler(this::handle)
        );
    }

    private void handle(CommandContext<Sender> context) {
        Sender sender = context.sender();
        String srcId = context.get("src");
        String dstId = context.get("dst");
        boolean delete = context.flags().hasFlag("delete");

        DataStoreLifecycle lifecycle = GrimAPI.INSTANCE.getDataStoreLifecycle();
        // Disabled in database.yml, or start() failed — either way there's
        // no backend pool to copy between.
        if (!lifecycle.isLoaded()) {
            sender.sendMessage(MessageUtil.miniMessage("%prefix% &cHistory subsystem not loaded!"));
            return;
        }

        Map<String, Backend> backends = lifecycle.allBackendsForCommands();
        Backend src = backends.get(srcId);
        Backend dst = backends.get(dstId);
        if (src == null) {
            sender.sendMessage(Component.text("Unknown source backend: " + srcId
                    + "  (configured: " + backends.keySet() + ")", NamedTextColor.RED));
            return;
        }
        if (dst == null) {
            sender.sendMessage(Component.text("Unknown destination backend: " + dstId
                    + "  (configured: " + backends.keySet() + ")", NamedTextColor.RED));
            return;
        }
        if (src == dst) {
            sender.sendMessage(Component.text(
                    "Source and destination are the same backend; refusing to copy.", NamedTextColor.RED));
            return;
        }

        logBoth(sender, Component.text()
                .append(Component.text("Copying v1 history ", NamedTextColor.AQUA))
                .append(Component.text(srcId, NamedTextColor.WHITE))
                .append(Component.text(" → ", NamedTextColor.AQUA))
                .append(Component.text(dstId, NamedTextColor.WHITE))
                .build());

        try {
            BackendToBackendCopier copier = new BackendToBackendCopier(src, dst);
            BackendToBackendCopier.Result result = copier.run(count -> {
                if (count > 0 && count % 5000 == 0) {
                    logBoth(sender, Component.text("… " + count + " violations copied so far", NamedTextColor.GRAY));
                }
            });
            logBoth(sender, Component.text()
                    .append(Component.text("Copy complete: ", NamedTextColor.GREEN))
                    .append(Component.text(result.players() + " players, "))
                    .append(Component.text(result.sessions() + " sessions, "))
                    .append(Component.text(result.violations() + " violations in "))
                    .append(Component.text(result.elapsedMs() + "ms"))
                    .build());
            if (delete) {
                logBoth(sender, Component.text("--delete requested — wiping source " + srcId + "…", NamedTextColor.YELLOW));
                copier.dropSource();
                logBoth(sender, Component.text("Source " + srcId + " wiped.", NamedTextColor.GREEN));
            }
        } catch (BackendException e) {
            logBoth(sender, Component.text("Copy failed: " + e.getMessage(), NamedTextColor.RED));
            LogUtil.error("v1 copy failed via /grim history copy", e);
        }
    }

    private static void logBoth(Sender sender, Component msg) {
        sender.sendMessage(msg);
        LogUtil.info(plain(msg));
    }

    private static String plain(Component c) {
        StringBuilder sb = new StringBuilder();
        flatten(c, sb);
        return sb.toString();
    }

    private static void flatten(Component c, StringBuilder sb) {
        if (c instanceof net.kyori.adventure.text.TextComponent tc) sb.append(tc.content());
        for (Component child : c.children()) flatten(child, sb);
    }
}
