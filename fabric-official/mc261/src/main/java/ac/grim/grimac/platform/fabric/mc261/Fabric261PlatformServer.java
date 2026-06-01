package ac.grim.grimac.platform.fabric.mc261;

import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.fabric.AbstractFabricPlatformServer;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import net.minecraft.commands.CommandSourceStack;

public class Fabric261PlatformServer extends AbstractFabricPlatformServer {
    @Override
    public double getTPS() {
        // 26.X retains the 1.20.3+ accessors. tickRateManager().tickrate() = the
        // configured tickrate cap (default 20.0); smoothed-tick-time gives the
        // actual achieved cadence in ms — take the min so we never report > cap.
        return Math.min(1000.0 / GrimACFabricLoaderPlugin.FABRIC_SERVER.getCurrentSmoothedTickTime(),
                GrimACFabricLoaderPlugin.FABRIC_SERVER.tickRateManager().tickrate());
    }

    @Override
    public void dispatchCommand(Sender sender, String command) {
        CommandSourceStack stack = (CommandSourceStack) GrimACFabricLoaderPlugin.LOADER.getFabricSenderFactory().unwrap(sender);
        GrimACFabricLoaderPlugin.FABRIC_SERVER.getCommands().performPrefixedCommand(stack, command);
    }
}
