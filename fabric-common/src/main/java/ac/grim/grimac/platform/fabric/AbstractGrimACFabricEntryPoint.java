package ac.grim.grimac.platform.fabric;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.fabric.initables.FabricBStats;
import ac.grim.grimac.platform.fabric.initables.FabricTickEndEvent;
import ac.grim.grimac.platform.fabric.inject.FabricMinecraftServerHandle;
import ac.grim.grimac.platform.fabric.scheduler.FabricPlatformScheduler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

import java.util.List;
import java.util.Objects;

public abstract class AbstractGrimACFabricEntryPoint<P extends AbstractGrimACFabricLoaderPlugin<?, ?, ?, ?, ?, ?>>
        implements PreLaunchEntrypoint, ModInitializer {
    private static volatile FabricMinecraftServerHandle server;

    @Override
    public void onPreLaunch() {
    }

    public static FabricMinecraftServerHandle server() {
        return Objects.requireNonNull(server);
    }

    public static FabricMinecraftServerHandle serverOrNull() {
        return server;
    }

    protected void initialize(
            String entryPointName,
            Class<P> pluginClass,
            boolean allowMissingEntryPoint
    ) {
        List<P> entryPoints = FabricLoader.getInstance().getEntrypoints(entryPointName, pluginClass);
        entryPoints.sort((a, b) -> b.getNativeVersion().getProtocolVersion() - a.getNativeVersion().getProtocolVersion());

        if (entryPoints.isEmpty()) {
            if (allowMissingEntryPoint) return;
            throw new IllegalStateException("No Fabric platform entrypoint found for " + entryPointName);
        }

        P platformLoader = entryPoints.get(0);
        setPlatformLoader(platformLoader);

        GrimAPI.INSTANCE.load(
                platformLoader,
                new FabricBStats(),
                new FabricTickEndEvent()
        );

        GrimAPI.INSTANCE.getCommandService().registerCommands();

        FabricServerEvents.onServerStarting(nativeServer -> {
            setNativeServer(nativeServer);
            server = nativeServer;
            GrimAPI.INSTANCE.start();
        });

        FabricServerEvents.onServerStopping(nativeServer -> {
            GrimAPI.INSTANCE.stop();
            ((FabricPlatformScheduler) platformLoader.getScheduler()).shutdown();
        });
    }

    protected abstract void setPlatformLoader(P platformLoader);

    protected abstract void setNativeServer(FabricMinecraftServerHandle server);
}
