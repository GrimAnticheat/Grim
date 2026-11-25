package ac.grim.grimac.platform.fabric;

import ac.grim.grimac.platform.fabric.inject.FabricMinecraftServerHandle;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

public class GrimACFabricIntermediaryEntryPoint extends AbstractGrimACFabricEntryPoint<GrimACFabricIntermediaryLoaderPlugin> {
    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(FabricServerEvents::fireServerStarting);
        ServerLifecycleEvents.SERVER_STOPPING.register(FabricServerEvents::fireServerStopping);
        ServerTickEvents.END_SERVER_TICK.register(FabricServerEvents::fireEndTick);
        initialize(
                "grimMainLoad",
                GrimACFabricIntermediaryLoaderPlugin.class,
                false
        );
    }

    @Override
    protected void setPlatformLoader(GrimACFabricIntermediaryLoaderPlugin platformLoader) {
        GrimACFabricIntermediaryLoaderPlugin.LOADER = platformLoader;
    }

    @Override
    protected void setNativeServer(FabricMinecraftServerHandle server) {
        GrimACFabricIntermediaryLoaderPlugin.FABRIC_SERVER = (MinecraftServer) server;
    }
}
