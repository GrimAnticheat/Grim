package ac.grim.grimac.platform.fabric.mc1161;

import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.fabric.AbstractFabricPlatformServer;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.NotNull;

public class Fabric1140PlatformServer extends AbstractFabricPlatformServer {

    @Override
    public void dispatchCommand(Sender sender, String command) {
        CommandSourceStack commandSource = GrimACFabricLoaderPlugin.LOADER.getFabricSenderFactory().reverse(sender);
        GrimACFabricLoaderPlugin.FABRIC_SERVER.getCommands().performCommand(commandSource, command);
    }

    @Override
    public void registerOutgoingPluginChannel(@NotNull String name) {
        // Just a placeholder. This will be overwritten from version 1.21.8
    }

    // TODO (Cross-platform) implement proper bukkit equivalent for getting TPS over time
    @Override
    public double getTPS() {
        return Math.min(1000.0 / GrimACFabricLoaderPlugin.FABRIC_SERVER.getAverageTickTime(), 20.0);
    }
}
