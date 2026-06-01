package ac.grim.grimac.platform.fabric;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.fabric.initables.FabricBStats;
import ac.grim.grimac.platform.fabric.initables.FabricTickEndEvent;
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

        // 26.X: cloud-fabric not yet ported, so getCommandService() returns no-op
        // (registerCommands is still safe to call but does nothing).
        GrimAPI.INSTANCE.getCommandService().registerCommands();

        // fabric-api event modules are intermediary-bound; FabricServerEvents is the
        // local replacement. A mixin into MinecraftServer (Phase B) drives the
        // listener lists below at the corresponding lifecycle points.
        FabricServerEvents.onServerStarting(server -> {
            GrimACFabricLoaderPlugin.FABRIC_SERVER = server;
            GrimAPI.INSTANCE.start();
        });

        FabricServerEvents.onServerStopping(server -> {
            GrimAPI.INSTANCE.stop();
            platformLoader.getScheduler().shutdown();
        });
    }
}
