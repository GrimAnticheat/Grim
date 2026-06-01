package ac.grim.grimac.platform.fabric;

import ac.grim.grimac.platform.api.PlatformServer;
import ac.grim.grimac.platform.api.sender.Sender;
import com.mojang.authlib.GameProfile;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractFabricPlatformServer implements PlatformServer {

    public int getOperatorPermissionLevel() {
        return GrimACFabricLoaderPlugin.FABRIC_SERVER.getOperatorUserPermissionLevel();
    }

    public boolean hasPermission(CommandSourceStack stack, int level) {
        return stack.hasPermission(level);
    }

    @Override
    public String getPlatformImplementationString() {
        // Return the Fabric server version
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
        return GrimACFabricLoaderPlugin.FABRIC_SERVER.getProfileCache().get(name);
    }
}
