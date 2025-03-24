package ac.grim.grimac.platform.fabric;

import ac.grim.grimac.platform.api.PlatformServer;
import ac.grim.grimac.platform.api.sender.Sender;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;


public class FabricPlatformServer implements PlatformServer {

    @Override
    public String getPlatformImplementationString() {
        // Return the Fabric server version
        return "Fabric " + FabricLoader.getInstance().getModContainer("fabricloader").get().getMetadata().getVersion().getFriendlyString() + " (MC: " + GrimACFabricLoaderPlugin.FABRIC_SERVER.getVersion() + ")";
    }

    @Override
    public void dispatchCommand(Sender sender, String command) {
        ServerCommandSource commandSource = GrimACFabricLoaderPlugin.LOADER.getFabricSenderFactory().reverse(sender);
        GrimACFabricLoaderPlugin.FABRIC_SERVER.getCommandManager().executeWithPrefix(commandSource, command);
    }

    @Override
    public Sender getConsoleSender() {
        ServerCommandSource consoleSource = GrimACFabricLoaderPlugin.FABRIC_SERVER.getCommandSource();
        return GrimACFabricLoaderPlugin.LOADER.getFabricSenderFactory().map(consoleSource);
    }

    @Override
    public void registerOutgoingPluginChannel(String bungeeCord) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getTPS() {
        return GrimACFabricLoaderPlugin.FABRIC_SERVER.getAverageTickTime();
    }
}
