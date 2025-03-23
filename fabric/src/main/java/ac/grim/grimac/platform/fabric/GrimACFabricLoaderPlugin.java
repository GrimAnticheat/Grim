package ac.grim.grimac.platform.fabric;

import ac.grim.grimac.BasicGrimPlugin;
import ac.grim.grimac.api.GrimPlugin;
import ac.grim.grimac.platform.api.PlatformLoader;
import ac.grim.grimac.platform.api.PlatformServer;
import ac.grim.grimac.platform.api.manager.ItemResetHandler;
import ac.grim.grimac.platform.api.manager.MessagePlaceHolderManager;
import ac.grim.grimac.platform.api.manager.ParserDescriptorFactory;
import ac.grim.grimac.platform.api.manager.PermissionRegistrationManager;
import ac.grim.grimac.platform.api.manager.PlatformPluginManager;
import ac.grim.grimac.platform.api.player.PlatformPlayerFactory;
import ac.grim.grimac.platform.api.scheduler.PlatformScheduler;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.api.sender.SenderFactory;
import ac.grim.grimac.platform.fabric.command.FabricPlayerSelectorAdapter;
import ac.grim.grimac.platform.fabric.command.FabricPlayerSelectorParser;
import ac.grim.grimac.platform.fabric.entity.FabricGrimEntity;
import ac.grim.grimac.platform.fabric.manager.FabricItemResetHandler;
import ac.grim.grimac.platform.fabric.manager.FabricMessagePlaceHolderManager;
import ac.grim.grimac.platform.fabric.manager.FabricParserDescriptorFactory;
import ac.grim.grimac.platform.fabric.manager.FabricPermissionRegistrationManager;
import ac.grim.grimac.platform.fabric.manager.FabricPlatformPluginManager;
import ac.grim.grimac.platform.fabric.player.FabricPlatformInventory;
import ac.grim.grimac.platform.fabric.player.FabricPlatformPlayer;
import ac.grim.grimac.platform.fabric.player.FabricPlatformPlayerFactory;
import ac.grim.grimac.platform.fabric.scheduler.FabricPlatformScheduler;
import ac.grim.grimac.platform.fabric.sender.FabricSenderFactory;
import ac.grim.grimac.utils.lazy.LazyHolder;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.fabric.FabricServerCommandManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class GrimACFabricLoaderPlugin implements PreLaunchEntrypoint, ModInitializer, PlatformLoader {
    public static MinecraftServer FABRIC_SERVER;
    public static GrimACFabricLoaderPlugin PLUGIN;
    protected final Logger logger = Logger.getLogger(GrimACFabricLoaderPlugin.class.getName());

    protected final LazyHolder<FabricPlatformScheduler> scheduler = LazyHolder.simple(FabricPlatformScheduler::new);
    // Since we JiJ PacketEvents and depend on it on Fabric, we can always just get the API instance since it loads firsts
    protected final PacketEventsAPI<?> packetEvents = PacketEvents.getAPI();
    protected final LazyHolder<FabricSenderFactory> senderFactory = LazyHolder.simple(FabricSenderFactory::new);
    protected final LazyHolder<CommandManager<Sender>> commandManager = LazyHolder.simple(this::createCommandManager);
    protected final LazyHolder<ItemResetHandler> itemResetHandler = LazyHolder.simple(FabricItemResetHandler::new);
    protected final LazyHolder<GrimPlugin> plugin = LazyHolder.simple(() ->
            new BasicGrimPlugin(
                    this.logger,
                    new File(FabricLoader.getInstance().getConfigDir().toFile(), "GrimAC"),
                    FabricLoader.getInstance().getModContainer("grimac").get().getMetadata().getVersion().getFriendlyString(),
                    FabricLoader.getInstance().getModContainer("grimac").get().getMetadata().getDescription(),
                    FabricLoader.getInstance().getModContainer("grimac").get().getMetadata().getAuthors().stream().map(Person::getName).collect(Collectors.toList())
            )
    );

    protected final PlatformPlayerFactory playerFactory = new FabricPlatformPlayerFactory(
            FabricPlatformPlayer::new,
            FabricGrimEntity::new,
            FabricPlatformInventory::new
    );
    protected final ParserDescriptorFactory parserFactory = new FabricParserDescriptorFactory(
            new FabricPlayerSelectorParser<>(FabricPlayerSelectorAdapter::new)
    );
    protected final PlatformPluginManager platformPluginManager = new FabricPlatformPluginManager();
    protected final PlatformServer platformServer = new FabricPlatformServer();
    protected final MessagePlaceHolderManager messagePlaceHolderManager = new FabricMessagePlaceHolderManager();
    protected final LazyHolder<FabricPermissionRegistrationManager> fabricPermissionRegistrationManager = LazyHolder.simple(FabricPermissionRegistrationManager::new);


    @Override
    public void onPreLaunch() {
    }

    @Override
    public void onInitialize() {
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

    @Override
    public PermissionRegistrationManager getPermissionManager() {
        return fabricPermissionRegistrationManager.get();
    }

    private CommandManager<Sender> createCommandManager() {
        return new FabricServerCommandManager<>(
                ExecutionCoordinator.simpleCoordinator(),
                senderFactory.get()
        );
    }

    public FabricSenderFactory getFabricSenderFactory() {
        return senderFactory.get();
    }
}
