package ac.grim.grimac.platform.fabric.mc1194;

import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import ac.grim.grimac.platform.fabric.mc1171.Fabric1171PlatformServer;
import net.minecraft.server.command.ServerCommandSource;

public class Fabric1190PlatformServer extends Fabric1171PlatformServer {
    @Override
    public void dispatchCommand(Sender sender, String command) {
        ServerCommandSource commandSource = GrimACFabricLoaderPlugin.LOADER.getFabricSenderFactory().reverse(sender);
        GrimACFabricLoaderPlugin.FABRIC_SERVER.getCommandManager().executeWithPrefix(commandSource, command);
    }
}
