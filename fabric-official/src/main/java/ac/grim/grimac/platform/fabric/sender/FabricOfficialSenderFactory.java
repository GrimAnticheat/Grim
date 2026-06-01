package ac.grim.grimac.platform.fabric.sender;

import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.fabric.AbstractFabricPlatformServer;
import ac.grim.grimac.platform.fabric.GrimACFabricOfficialLoaderPlugin;
import ac.grim.grimac.platform.fabric.utils.message.IFabricMessageUtil;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.util.TriState;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.rcon.RconConsoleSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public class FabricOfficialSenderFactory extends AbstractFabricSenderFactory<CommandSourceStack> {

    private final AbstractFabricPlatformServer platformServer = GrimACFabricOfficialLoaderPlugin.LOADER.getPlatformServer();
    private final IFabricMessageUtil fabricMessageUtils = GrimACFabricOfficialLoaderPlugin.LOADER.getFabricMessageUtils();

    @Override
    public @NotNull Sender wrap(@NotNull CommandSourceStack source) {
        return Objects.requireNonNull(source, "source");
    }

    @Override
    public UUID getUniqueId(CommandSourceStack source) {
        if (source.getEntity() != null) {
            return source.getEntity().getUUID();
        }
        return Sender.CONSOLE_UUID;
    }

    @Override
    public String getName(CommandSourceStack source) {
        String name = source.getTextName();
        if (source.getEntity() != null && name.equals("Server")) {
            return Sender.CONSOLE_NAME;
        }
        return name;
    }

    @Override
    public void sendMessage(CommandSourceStack source, String message) {
        fabricMessageUtils.sendMessage((Sender) (Object) source, fabricMessageUtils.textLiteral(message), false);
    }

    @Override
    public void sendMessage(CommandSourceStack source, Component message) {
        net.minecraft.network.chat.Component nativeText =
                (net.minecraft.network.chat.Component) GrimACFabricOfficialLoaderPlugin.LOADER.getFabricConversionUtil().toNativeText(message);
        fabricMessageUtils.sendMessage((Sender) (Object) source, nativeText, false);
    }

    @Override
    protected @Nullable Boolean queryPermissionValue(CommandSourceStack source, String node) {
        TriState value = Permissions.getPermissionValue(source, node);
        return value == TriState.DEFAULT ? null : value.get();
    }

    @Override
    protected boolean queryPermission(CommandSourceStack source, String node, boolean defaultIfUnset) {
        return Permissions.check(source, node, defaultIfUnset);
    }

    @Override
    protected boolean isOperator(CommandSourceStack source) {
        return source.permissions().hasPermission(
                new Permission.HasCommandLevel(PermissionLevel.byId(2)));
    }

    @Override
    public void performCommand(CommandSourceStack source, String command) {
        platformServer.dispatchCommand(source, command);
    }

    @Override
    public boolean isConsole(CommandSourceStack source) {
        CommandSource out = source.source;
        return out == source.getServer()
                || out.getClass() == RconConsoleSource.class
                || (out == CommandSource.NULL && source.getTextName().isEmpty());
    }

    @Override
    public boolean isPlayer(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer;
    }
}
