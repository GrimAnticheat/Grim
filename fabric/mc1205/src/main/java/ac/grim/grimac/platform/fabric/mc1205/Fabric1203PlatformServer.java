package ac.grim.grimac.platform.fabric.mc1205;

import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import ac.grim.grimac.platform.fabric.mc1194.Fabric1190PlatformServer;
import net.minecraft.commands.CommandSourceStack;

public class Fabric1203PlatformServer extends Fabric1190PlatformServer {

    // TODO (Cross-platform) implement proper bukkit equivalent for getting TPS over time
    @Override
    public double getTPS() {
        return Math.min(1000.0 / GrimACFabricLoaderPlugin.FABRIC_SERVER.getCurrentSmoothedTickTime(), GrimACFabricLoaderPlugin.FABRIC_SERVER.tickRateManager().tickrate());
    }

    // Return type changed from int -> void in 1.20.3
    @Override
    public void dispatchCommand(Sender sender, String command) {
        CommandSourceStack commandSource = GrimACFabricLoaderPlugin.LOADER.getFabricSenderFactory().unwrap(sender);
        GrimACFabricLoaderPlugin.FABRIC_SERVER.getCommands().performPrefixedCommand(commandSource, command);
    }
}
