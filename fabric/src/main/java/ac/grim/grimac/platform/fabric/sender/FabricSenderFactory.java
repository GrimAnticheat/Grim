package ac.grim.grimac.platform.fabric.sender;

import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.api.sender.SenderFactory;
import io.github.retrooper.packetevents.adventure.serializer.gson.GsonComponentSerializer;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.kyori.adventure.text.Component;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.rcon.RconCommandOutput;
import net.minecraft.text.Text;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.SenderMapper;

import java.util.UUID;

public class FabricSenderFactory extends SenderFactory<ServerCommandSource> implements SenderMapper<ServerCommandSource, Sender> {

    @Override
    protected UUID getUniqueId(ServerCommandSource commandSource) {
        if (commandSource.getEntity() != null) {
            return commandSource.getEntity().getUuid();
        }
        return Sender.CONSOLE_UUID;
    }

    @Override
    protected String getName(ServerCommandSource commandSource) {
        String name = commandSource.getName();
        if (commandSource.getEntity() != null && name.equals("Server")) {
            return Sender.CONSOLE_NAME;
        }
        return name;
    }

    @Override
    protected void sendMessage(ServerCommandSource sender, String message) {
        sender.sendFeedback(() -> Text.literal(message), false);
    }

    @Override
    protected void sendMessage(ServerCommandSource sender, Component message) {
        sender.sendFeedback(() -> toNativeText(message), false);
    }

    @Override
    protected boolean hasPermission(ServerCommandSource commandSource, String node) {
        return Permissions.check(commandSource, node);
    }

    @Override
    protected boolean hasPermission(ServerCommandSource commandSource, String node, boolean defaultIfUnset) {
        return Permissions.check(commandSource, node, defaultIfUnset);
    }

    @Override
    protected void performCommand(ServerCommandSource sender, String command) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean isConsole(ServerCommandSource sender) {
        CommandOutput output = sender.output;
        return output == sender.getServer() || // Console
                output.getClass() == RconCommandOutput.class || // Rcon
                (output == CommandOutput.DUMMY && sender.getName().equals("")); // Functions
    }

    @Override
    public @NonNull Sender map(@NonNull ServerCommandSource base) {
        return this.wrap(base);
    }

    @Override
    public @NonNull ServerCommandSource reverse(@NonNull Sender mapped) {
        return this.unwrap(mapped);
    }

    @Override
    public void close() throws Exception {
        throw new UnsupportedOperationException();
    }

    public static Text toNativeText(Component component) {
        return Text.Serialization.fromJsonTree(GsonComponentSerializer.gson().serializeToTree(component), DynamicRegistryManager.EMPTY);
    }
}
