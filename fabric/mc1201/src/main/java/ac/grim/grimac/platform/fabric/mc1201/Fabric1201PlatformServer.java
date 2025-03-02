package ac.grim.grimac.platform.fabric.mc1201;


import ac.grim.grimac.platform.fabric.FabricPlatformServer;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;

public class Fabric1201PlatformServer extends FabricPlatformServer {

    @Override
    public double getTPS() {
        return GrimACFabricLoaderPlugin.FABRIC_SERVER.getTickTime();
    }
}
