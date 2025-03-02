package ac.grim.grimac.platform.fabric.mc1214;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.start.CommandRegister;
import ac.grim.grimac.platform.fabric.initables.FabricBStats;
import ac.grim.grimac.platform.fabric.initables.FabricTickEndEvent;
import com.github.retrooper.packetevents.PacketEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class GrimACFabricLoaderPlugin extends ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin {
    @Override
    public void onPreLaunch() {
        PLUGIN = this;
        PacketEvents.setAPI(packetEvents);
    }

    @Override
    public void onInitialize() {
        // On Fabric we have to register commands earlier, and cannot register them when server is no longer null
        GrimAPI.INSTANCE.load(
                this,
                new FabricBStats(),
                new FabricTickEndEvent()
        );

        CommandRegister.registerCommands(commandManager.get());

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            FABRIC_SERVER = server;
            GrimAPI.INSTANCE.start();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register((server) -> {
            GrimAPI.INSTANCE.stop();
            this.scheduler.get().shutdown();
        });
    }
}
