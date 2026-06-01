package ac.grim.grimac.platform.fabric;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.fabric.initables.FabricBStats;
import ac.grim.grimac.platform.fabric.initables.FabricTickEndEvent;
import ac.grim.grimac.platform.fabric.inject.FabricServerHolder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

import java.util.List;

public class GrimACFabricEntryPoint implements PreLaunchEntrypoint, ModInitializer {
    @Override
    public void onPreLaunch() {
    }

    @Override
    public void onInitialize() {
        FabricLoader loader = FabricLoader.getInstance();
        String chainLoadEntryPointName = "grim26MainLoad";

        List<GrimACFabricLoaderPlugin> mainChainLoadEntryPoints = loader.getEntrypoints(chainLoadEntryPointName, GrimACFabricLoaderPlugin.class);
        mainChainLoadEntryPoints.sort((a, b) -> b.getNativeVersion().getProtocolVersion() - a.getNativeVersion().getProtocolVersion());

        if (mainChainLoadEntryPoints.isEmpty()) return;

        GrimACFabricLoaderPlugin platformLoader = mainChainLoadEntryPoints.get(0);
        GrimACFabricLoaderPlugin.LOADER = platformLoader;

        GrimAPI.INSTANCE.load(
                platformLoader,
                new FabricBStats(),
                new FabricTickEndEvent()
        );

        GrimAPI.INSTANCE.getCommandService().registerCommands();

        // Server lifecycle is driven by MinecraftServerMixin into the FabricServerEvents
        // shim (avoids a hard fabric-api dependency for two hooks the mixin provides).
        FabricServerEvents.onServerStarting(server -> {
            GrimACFabricLoaderPlugin.FABRIC_SERVER = server;
            // Mirror into the NMS-free holder so shared fabric-common code (e.g.
            // FabricOfflinePlatformPlayer) can reach the server via FabricMinecraftServerHandle.
            FabricServerHolder.set(server);
            GrimAPI.INSTANCE.start();
        });

        FabricServerEvents.onServerStopping(server -> {
            GrimAPI.INSTANCE.stop();
            platformLoader.getScheduler().shutdown();
        });
    }
}
