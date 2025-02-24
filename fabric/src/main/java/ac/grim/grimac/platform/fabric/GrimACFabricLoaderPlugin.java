package ac.grim.grimac.platform.fabric;

import ac.grim.grimac.BasicGrimPlugin;
import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.GrimPlugin;
import ac.grim.grimac.manager.init.start.CommandRegister;
import ac.grim.grimac.platform.api.PlatformLoader;
import ac.grim.grimac.platform.api.PlatformServer;
import ac.grim.grimac.platform.api.manager.ItemResetHandler;
import ac.grim.grimac.platform.api.manager.MessagePlaceHolderManager;
import ac.grim.grimac.platform.api.manager.ParserDescriptorFactory;
import ac.grim.grimac.platform.api.manager.PlatformPluginManager;
import ac.grim.grimac.platform.api.player.PlatformPlayerFactory;
import ac.grim.grimac.platform.api.scheduler.PlatformScheduler;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.api.sender.SenderFactory;
import ac.grim.grimac.platform.fabric.initables.FabricBStats;
import ac.grim.grimac.platform.fabric.initables.FabricTickEndEvent;
import ac.grim.grimac.platform.fabric.manager.FabricItemResetHandler;
import ac.grim.grimac.platform.fabric.manager.FabricMessagePlaceHolderManager;
import ac.grim.grimac.platform.fabric.manager.FabricParserDescriptorFactory;
import ac.grim.grimac.platform.fabric.manager.FabricPlatformPluginManager;
import ac.grim.grimac.platform.fabric.player.FabricPlatformPlayerFactory;
import ac.grim.grimac.platform.fabric.scheduler.FabricPlatformScheduler;
import ac.grim.grimac.platform.fabric.sender.FabricSenderFactory;
import ac.grim.grimac.utils.lazy.LazyHolder;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import io.github.retrooper.packetevents.factory.fabric.FabricPacketEventsAPI;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.fabric.FabricCommandManager;
import org.incendo.cloud.fabric.FabricServerCommandManager;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;
import java.util.stream.Collectors;

public class GrimACFabricLoaderPlugin implements PreLaunchEntrypoint, ModInitializer, PlatformLoader {
    public static MinecraftServer FABRIC_SERVER;
    public static GrimACFabricLoaderPlugin PLUGIN;
    private final Logger logger = Logger.getLogger(GrimACFabricLoaderPlugin.class.getName());

    private final LazyHolder<PlatformScheduler> scheduler = LazyHolder.simple(FabricPlatformScheduler::new);
    private final PacketEventsAPI<?> packetEvents = new FabricPacketEventsAPI("grimac", EnvType.SERVER);
    private final LazyHolder<FabricSenderFactory> senderFactory = LazyHolder.simple(FabricSenderFactory::new);
    private final LazyHolder<CommandManager<Sender>> commandManager = LazyHolder.simple(this::createCommandManager);
    private final LazyHolder<ItemResetHandler> itemResetHandler = LazyHolder.simple(FabricItemResetHandler::new);
    private final LazyHolder<GrimPlugin> plugin = LazyHolder.simple(() ->
        new BasicGrimPlugin(
                this.logger,
                FabricLoader.getInstance().getConfigDir().toFile(),
                FabricLoader.getInstance().getModContainer("grimac").get().getMetadata().getVersion().getFriendlyString(),
                FabricLoader.getInstance().getModContainer("grimac").get().getMetadata().getDescription(),
                FabricLoader.getInstance().getModContainer("grimac").get().getMetadata().getAuthors().stream().map(Person::getName).collect(Collectors.toList())
        )
    );

    private final PlatformPlayerFactory playerFactory = new FabricPlatformPlayerFactory();
    private final ParserDescriptorFactory parserFactory = new FabricParserDescriptorFactory();
    private final PlatformPluginManager platformPluginManager = new FabricPlatformPluginManager();
    private final PlatformServer platformServer = new FabricPlatformServer();
    private final MessagePlaceHolderManager messagePlaceHolderManager = new FabricMessagePlaceHolderManager();


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
        });
    }

    @Override
    public PlatformScheduler getScheduler() {
        return scheduler.get();
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
    public PacketEventsAPI<?> getPacketEvents() {
        return packetEvents;
    }

    @Override
    public CommandManager<Sender> getCommandManager() {
        return commandManager.get();
    }

    @Override
    public ItemResetHandler getItemResetHandler() {
        return itemResetHandler.get();
    }

    @Override
    public SenderFactory<ServerCommandSource> getSenderFactory() {
        return senderFactory.get();
    }

    @Override
    public GrimPlugin getPlugin() {
        return plugin.get();
    }

    @Override
    public PlatformPluginManager getPluginManager() {
        return platformPluginManager;
    }

    @Override
    public PlatformServer getPlatformServer() {
        return platformServer;
    }

    // TODO (Cross-platform) (Fabric) implement getting the API
    @Override
    public void registerAPIService() {

    }

    @Override
    public @NotNull MessagePlaceHolderManager getMessagePlaceHolderManager() {
        return messagePlaceHolderManager;
    }

    private CommandManager<Sender> createCommandManager() {
        FabricCommandManager<Sender, ServerCommandSource> manager = new FabricServerCommandManager<>(
                ExecutionCoordinator.simpleCoordinator(),
                senderFactory.get()
        );
        return manager;
    }

    public FabricSenderFactory getFabricSenderFactory() {
        return senderFactory.get();
    }
}
