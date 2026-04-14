package ac.grim.grimac.platform.fabric;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.api.PlatformLoader;
import ac.grim.grimac.platform.fabric.initables.FabricBStats;
import ac.grim.grimac.platform.fabric.initables.FabricTickEndEvent;
import ac.grim.grimac.platform.fabric.scheduler.FabricPlatformScheduler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.api.metadata.ModMetadata;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GrimACFabricEntryPoint implements PreLaunchEntrypoint, ModInitializer {
    @Override
    public void onPreLaunch() {
    }

    @Override
    public void onInitialize() {
        FabricLoader loader = FabricLoader.getInstance();
        String chainLoadEntryPointName = "grimMainLoad";

        // getEntrypoints() instantiates every provider; version slices share loose "minecraft" lower bounds and must not all load on one game version.
        // Use PlatformLoader (from :common on the root jar) so we do not force-load GrimACFabricLoaderPlugin on the root mod classloader: that type must
        // come from the nested slice only, or Mojang-mapped copies on the root break linkage with intermediary slice bytecode on 1.21.x.
        List<EntrypointContainer<PlatformLoader>> containers = new ArrayList<>(
                loader.getEntrypointContainers(chainLoadEntryPointName, PlatformLoader.class));
        if (containers.isEmpty()) {
            throw new IllegalStateException(
                    "GrimAC: no Fabric grimMainLoad entrypoints matched this Minecraft version. "
                            + "The aggregate mod must embed a version-specific slice (e.g. mc12111 for 1.21.x) whose fabric.mod.json registers grimMainLoad."
            );
        }
        containers.sort(Comparator.comparingInt((EntrypointContainer<PlatformLoader> c) -> -loaderPriority(c.getProvider().getMetadata())));

        PlatformLoader platformLoader = null;
        List<Throwable> failures = new ArrayList<>();
        for (EntrypointContainer<PlatformLoader> container : containers) {
            try {
                platformLoader = container.getEntrypoint();
                break;
            } catch (Throwable t) {
                failures.add(t);
            }
        }
        if (platformLoader == null) {
            IllegalStateException ex = new IllegalStateException(
                    "GrimAC: no compatible grimMainLoad Fabric loader could be constructed for this Minecraft version (tried "
                            + containers.size()
                            + " slice(s)).");
            failures.forEach(ex::addSuppressed);
            throw ex;
        }
        final PlatformLoader chosenLoader = platformLoader;
        assignFabricLoaderStatic(chosenLoader);

        // On Fabric we have to register commands earlier, and cannot register them when server is no longer null
        GrimAPI.INSTANCE.load(
                chosenLoader,
                new FabricBStats(),
                new FabricTickEndEvent()
        );

        GrimAPI.INSTANCE.getCommandService().registerCommands();

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            assignFabricServerStatic(chosenLoader, server);
            GrimAPI.INSTANCE.start();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register((server) -> {
            GrimAPI.INSTANCE.stop();
            ((FabricPlatformScheduler) chosenLoader.getScheduler()).shutdown();
        });
    }

    private static void assignFabricLoaderStatic(PlatformLoader chosenLoader) {
        ClassLoader cl = chosenLoader.getClass().getClassLoader();
        try {
            Class<?> cls = Class.forName("ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin", true, cl);
            Field loaderField = cls.getDeclaredField("LOADER");
            loaderField.setAccessible(true);
            loaderField.set(null, chosenLoader);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("GrimAC: failed to set GrimACFabricLoaderPlugin.LOADER on slice classloader", e);
        }
    }

    private static void assignFabricServerStatic(PlatformLoader chosenLoader, Object server) {
        ClassLoader cl = chosenLoader.getClass().getClassLoader();
        try {
            Class<?> cls = Class.forName("ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin", true, cl);
            Field serverField = cls.getDeclaredField("FABRIC_SERVER");
            serverField.setAccessible(true);
            serverField.set(null, server);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("GrimAC: failed to set GrimACFabricLoaderPlugin.FABRIC_SERVER", e);
        }
    }

    /**
     * Higher value = newer slice; tried first so only the matching slice is constructed.
     */
    private static int loaderPriority(ModMetadata meta) {
        String id = meta.getId();
        if ("grimac-fabric-official-mc261".equals(id)) {
            return 1_000_000;
        }
        if (id.startsWith("grimac-mc")) {
            String suffix = id.substring("grimac-mc".length());
            try {
                return Integer.parseInt(suffix);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
}
