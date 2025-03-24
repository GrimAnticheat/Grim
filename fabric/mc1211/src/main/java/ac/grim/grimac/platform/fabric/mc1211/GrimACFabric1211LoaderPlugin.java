package ac.grim.grimac.platform.fabric.mc1211;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.start.CommandRegister;
import ac.grim.grimac.platform.api.manager.ParserDescriptorFactory;
import ac.grim.grimac.platform.api.player.PlatformPlayerFactory;
import ac.grim.grimac.platform.fabric.command.FabricPlayerSelectorParser;
import ac.grim.grimac.platform.fabric.initables.FabricBStats;
import ac.grim.grimac.platform.fabric.initables.FabricTickEndEvent;
import ac.grim.grimac.platform.fabric.manager.FabricParserDescriptorFactory;
import ac.grim.grimac.platform.fabric.mc1211.command.Fabric1211PlayerSelectorAdapter;
import ac.grim.grimac.platform.fabric.mc1211.entity.Fabric1211GrimEntity;
import ac.grim.grimac.platform.fabric.mc1211.player.Fabric1211PlatformPlayer;
import ac.grim.grimac.platform.fabric.player.FabricPlatformInventory;
import ac.grim.grimac.platform.fabric.player.FabricPlatformPlayerFactory;
import com.github.retrooper.packetevents.PacketEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class GrimACFabric1211LoaderPlugin extends ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin {

    protected final PlatformPlayerFactory playerFactory = new FabricPlatformPlayerFactory(
            Fabric1211PlatformPlayer::new,
            Fabric1211GrimEntity::new,
            FabricPlatformInventory::new
    );
    protected final ParserDescriptorFactory parserFactory = new FabricParserDescriptorFactory(
            new FabricPlayerSelectorParser<>(Fabric1211PlayerSelectorAdapter::new)
    );

    @Override
    public void onPreLaunch() {
        LOADER = this;
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

    @Override
    public PlatformPlayerFactory getPlatformPlayerFactory() {
        return playerFactory;
    }

    @Override
    public ParserDescriptorFactory getParserDescriptorFactory() {
        return parserFactory;
    }
}
