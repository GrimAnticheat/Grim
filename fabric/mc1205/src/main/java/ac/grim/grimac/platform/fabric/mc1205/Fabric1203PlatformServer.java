package ac.grim.grimac.platform.fabric.mc1205;

import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import ac.grim.grimac.platform.fabric.mc1194.Fabric1190PlatformServer;
import net.minecraft.server.command.ServerCommandSource;

public class Fabric1203PlatformServer extends Fabric1190PlatformServer {

    // TODO (Cross-platform) implement proper bukkit equivalent for getting TPS over time
    @Override
    public double getTPS() {
        return GrimACFabricLoaderPlugin.FABRIC_SERVER.getAverageTickTime();
    }
}
