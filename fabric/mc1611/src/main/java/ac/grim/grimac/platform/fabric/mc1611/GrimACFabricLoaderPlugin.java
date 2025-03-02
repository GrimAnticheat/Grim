package ac.grim.grimac.platform.fabric.mc1611;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.start.CommandRegister;
import ac.grim.grimac.platform.api.manager.ParserDescriptorFactory;
import ac.grim.grimac.platform.api.player.PlatformPlayerFactory;
import ac.grim.grimac.platform.fabric.command.FabricPlayerSelectorParser;
import ac.grim.grimac.platform.fabric.initables.FabricBStats;
import ac.grim.grimac.platform.fabric.initables.FabricTickEndEvent;
import ac.grim.grimac.platform.fabric.manager.FabricParserDescriptorFactory;
import ac.grim.grimac.platform.fabric.mc1611.command.Fabric1611PlayerSelectorAdapter;
import ac.grim.grimac.platform.fabric.mc1611.entity.Fabric1611GrimEntity;
import ac.grim.grimac.platform.fabric.mc1611.player.Fabric1611PlatformInventory;
import ac.grim.grimac.platform.fabric.mc1611.player.Fabric1611PlatformPlayer;
import ac.grim.grimac.platform.fabric.mc1611.sender.Fabric1611SenderFactory;
import ac.grim.grimac.platform.fabric.mc1611.util.convert.Fabric1611ConversionUtil;
import ac.grim.grimac.platform.fabric.player.FabricPlatformPlayerFactory;
import ac.grim.grimac.platform.fabric.sender.FabricSenderFactory;
import ac.grim.grimac.platform.fabric.utils.convert.FabricConversionUtil;
import ac.grim.grimac.utils.lazy.LazyHolder;
import com.github.retrooper.packetevents.PacketEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class GrimACFabricLoaderPlugin extends ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin {

    protected final LazyHolder<FabricSenderFactory> senderFactory = LazyHolder.simple(Fabric1611SenderFactory::new);
    protected final PlatformPlayerFactory playerFactory = new FabricPlatformPlayerFactory(
            Fabric1611PlatformPlayer::new,
            Fabric1611GrimEntity::new,
            Fabric1611PlatformInventory::new
    );
    protected final ParserDescriptorFactory parserFactory = new FabricParserDescriptorFactory(
            new FabricPlayerSelectorParser<>(Fabric1611PlayerSelectorAdapter::new)
    );

    @Override
    public void onPreLaunch() {
        PLUGIN = this;
        PacketEvents.setAPI(packetEvents);
        FabricConversionUtil.setConverters(
                Fabric1611ConversionUtil::fromFabricItemStack,
                Fabric1611ConversionUtil::toNativeText
        );
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

    @Override
    public FabricSenderFactory getSenderFactory() {
        return this.senderFactory.get();
    }

    @Override
    public FabricSenderFactory getFabricSenderFactory() {
        return this.senderFactory.get();
    }
}
