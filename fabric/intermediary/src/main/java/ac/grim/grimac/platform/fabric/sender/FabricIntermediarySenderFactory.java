package ac.grim.grimac.platform.fabric.sender;

import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.fabric.AbstractFabricPlatformServer;
import ac.grim.grimac.platform.fabric.GrimACFabricIntermediaryLoaderPlugin;
import ac.grim.grimac.platform.fabric.utils.message.IFabricMessageUtil;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.util.TriState;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.rcon.RconConsoleSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public class FabricIntermediarySenderFactory extends AbstractFabricSenderFactory<CommandSourceStack> {

    private final AbstractFabricPlatformServer platformServer = GrimACFabricIntermediaryLoaderPlugin.LOADER.getPlatformServer();
    private final IFabricMessageUtil fabricMessageUtils = GrimACFabricIntermediaryLoaderPlugin.LOADER.getFabricMessageUtils();

    @Override
    public @NotNull Sender wrap(@NotNull CommandSourceStack sender) {
        return Objects.requireNonNull(sender, "sender");
    }

    @Override
    public UUID getUniqueId(CommandSourceStack commandSource) {
        if (commandSource.getEntity() != null) {
            return commandSource.getEntity().getUUID();
        }
        return Sender.CONSOLE_UUID;
    }

    @Override
    public String getName(CommandSourceStack commandSource) {
        String name = commandSource.getTextName();
        if (commandSource.getEntity() != null && name.equals("Server")) {
            return Sender.CONSOLE_NAME;
        }
        return name;
    }

    @Override
    public void sendMessage(CommandSourceStack sender, String message) {
        fabricMessageUtils.sendMessage((Sender) (Object) sender, fabricMessageUtils.textLiteral(message), false);
    }

    @Override
    public void sendMessage(CommandSourceStack sender, Component message) {
        net.minecraft.network.chat.Component nativeText =
                (net.minecraft.network.chat.Component) GrimACFabricIntermediaryLoaderPlugin.LOADER.getFabricConversionUtil().toNativeText(message);
        fabricMessageUtils.sendMessage((Sender) (Object) sender, nativeText, false);
    }

    @Override
    protected @Nullable Boolean queryPermissionValue(CommandSourceStack sender, String node) {
        TriState value = Permissions.getPermissionValue(sender, node);
        return value == TriState.DEFAULT ? null : value.get();
    }

    @Override
    protected boolean queryPermission(CommandSourceStack sender, String node, boolean defaultIfUnset) {
        return Permissions.check(sender, node, defaultIfUnset);
    }

    @Override
    protected boolean isOperator(CommandSourceStack sender) {
        return platformServer.hasPermission((Sender) (Object) sender, platformServer.getOperatorPermissionLevel());
    }

    @Override
    public void performCommand(CommandSourceStack sender, String command) {
        platformServer.dispatchCommand((Sender) (Object) sender, command);
    }

    @Override
    public boolean isConsole(CommandSourceStack sender) {
        CommandSource output = sender.source;
        return output == sender.getServer()
                || output.getClass() == RconConsoleSource.class
                || (output == CommandSource.NULL && sender.getTextName().isEmpty());
    }

    @Override
    public boolean isPlayer(CommandSourceStack sender) {
        return sender.getEntity() instanceof ServerPlayer;
    }
}
