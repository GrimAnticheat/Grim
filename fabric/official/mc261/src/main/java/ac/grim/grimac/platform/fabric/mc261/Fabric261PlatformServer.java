package ac.grim.grimac.platform.fabric.mc261;

import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.fabric.AbstractFabricPlatformServer;
import ac.grim.grimac.platform.fabric.GrimACFabricOfficialLoaderPlugin;
import ac.grim.grimac.platform.fabric.player.FabricOfflineProfile;
import com.mojang.authlib.GameProfile;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import org.jetbrains.annotations.Nullable;

public class Fabric261PlatformServer extends AbstractFabricPlatformServer {
    @Override
    public int getOperatorPermissionLevel() {
        return GrimACFabricOfficialLoaderPlugin.FABRIC_SERVER.operatorUserPermissions().level().id();
    }

    @Override
    public boolean hasPermission(Sender sender, int level) {
        CommandSourceStack stack = (CommandSourceStack) sender;
        return stack.permissions().hasPermission(
                new Permission.HasCommandLevel(PermissionLevel.byId(level)));
    }

    @Override
    public double getTPS() {
        return Math.min(1000.0 / GrimACFabricOfficialLoaderPlugin.FABRIC_SERVER.getCurrentSmoothedTickTime(),
                GrimACFabricOfficialLoaderPlugin.FABRIC_SERVER.tickRateManager().tickrate());
    }

    @Override
    public void dispatchCommand(Sender sender, String command) {
        CommandSourceStack stack = (CommandSourceStack) GrimACFabricOfficialLoaderPlugin.LOADER.getFabricSenderFactory().unwrap(sender);
        GrimACFabricOfficialLoaderPlugin.FABRIC_SERVER.getCommands().performPrefixedCommand(stack, command);
    }

    @Override
    public @Nullable FabricOfflineProfile getProfileByName(String name) {
        GameProfile profile = GrimACFabricOfficialLoaderPlugin.FABRIC_SERVER.services().profileResolver().fetchByName(name).orElse(null);
        return profile != null ? new FabricOfflineProfile(profile.id(), profile.name()) : null;
    }
}
