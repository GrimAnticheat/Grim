package ac.grim.grimac.platform.fabric.mc1611;

import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.fabric.FabricPlatformServer;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import net.minecraft.server.command.ServerCommandSource;


public class Fabric1611PlatformServer extends FabricPlatformServer {

    @Override
    public void dispatchCommand(Sender sender, String command) {
        ServerCommandSource commandSource = ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin.PLUGIN.getFabricSenderFactory().reverse(sender);
        ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin.FABRIC_SERVER.getCommandManager().execute(commandSource, command);
    }

    // TODO (Cross-platform) implement proper bukkit equivalent for getting TPS over time
    @Override
    public double getTPS() {
        return GrimACFabricLoaderPlugin.FABRIC_SERVER.getTickTime();
    }
}
