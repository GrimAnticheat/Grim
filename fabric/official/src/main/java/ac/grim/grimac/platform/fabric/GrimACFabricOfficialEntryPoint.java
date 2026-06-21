package ac.grim.grimac.platform.fabric;

import ac.grim.grimac.platform.fabric.inject.FabricMinecraftServerHandle;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

public class GrimACFabricOfficialEntryPoint extends AbstractGrimACFabricEntryPoint<GrimACFabricOfficialLoaderPlugin> {
    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(FabricServerEvents::fireServerStarting);
        ServerLifecycleEvents.SERVER_STOPPING.register(FabricServerEvents::fireServerStopping);
        ServerTickEvents.END_SERVER_TICK.register(FabricServerEvents::fireEndTick);
        initialize(
                "grim26MainLoad",
                GrimACFabricOfficialLoaderPlugin.class,
                true
        );
    }

    @Override
    protected void setPlatformLoader(GrimACFabricOfficialLoaderPlugin platformLoader) {
        GrimACFabricOfficialLoaderPlugin.LOADER = platformLoader;
    }

    @Override
    protected void setNativeServer(FabricMinecraftServerHandle server) {
        GrimACFabricOfficialLoaderPlugin.FABRIC_SERVER = (MinecraftServer) (Object) server;
    }
}
