package ac.grim.grimac.platform.fabric;

import ac.grim.grimac.platform.api.PlatformServer;
import ac.grim.grimac.platform.api.sender.Sender;
import com.mojang.authlib.GameProfile;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractFabricPlatformServer implements PlatformServer {

    public int getOperatorPermissionLevel() {
        // 26.X: getOperatorUserPermissionLevel() → operatorUserPermissions().level().id()
        return GrimACFabricLoaderPlugin.FABRIC_SERVER.operatorUserPermissions().level().id();
    }

    public boolean hasPermission(CommandSourceStack stack, int level) {
        // 26.X: hasPermission(int) → permissions().hasPermission(new Permission.HasCommandLevel(...))
        return stack.permissions().hasPermission(
                new Permission.HasCommandLevel(PermissionLevel.byId(level)));
    }

    @Override
    public String getPlatformImplementationString() {
        return "Fabric " + FabricLoader.getInstance().getModContainer("fabricloader").orElseThrow().getMetadata().getVersion().getFriendlyString() + " (MC: " + GrimACFabricLoaderPlugin.FABRIC_SERVER.getServerVersion() + ")";
    }

    @Override
    public Sender getConsoleSender() {
        CommandSourceStack consoleSource = GrimACFabricLoaderPlugin.FABRIC_SERVER.createCommandSourceStack();
        return GrimACFabricLoaderPlugin.LOADER.getFabricSenderFactory().wrap(consoleSource);
    }

    @Override
    public void registerOutgoingPluginChannel(String name) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    public GameProfile getProfileByName(String name) {
        // 26.X: getProfileCache().get(name) → services().profileResolver().fetchByName(name)
        return GrimACFabricLoaderPlugin.FABRIC_SERVER.services().profileResolver().fetchByName(name).orElse(null);
    }
}
