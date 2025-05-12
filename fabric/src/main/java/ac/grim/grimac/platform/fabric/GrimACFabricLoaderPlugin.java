package ac.grim.grimac.platform.fabric;

import ac.grim.grimac.api.plugin.BasicGrimPlugin;
import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.GrimAPIProvider;
import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.platform.api.PlatformLoader;
import ac.grim.grimac.platform.api.PlatformServer;
import ac.grim.grimac.platform.api.manager.*;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.api.sender.SenderFactory;
import ac.grim.grimac.platform.fabric.manager.*;
import ac.grim.grimac.platform.fabric.player.FabricPlatformPlayerFactory;
import ac.grim.grimac.platform.fabric.scheduler.FabricPlatformScheduler;
import ac.grim.grimac.platform.fabric.sender.FabricSenderFactory;
import ac.grim.grimac.platform.fabric.utils.convert.IFabricConversionUtil;
import ac.grim.grimac.platform.fabric.utils.message.IFabricMessageUtil;
import ac.grim.grimac.platform.fabric.utils.message.JULoggerFactory;
import ac.grim.grimac.utils.lazy.LazyHolder;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.fabric.FabricServerCommandManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.stream.Collectors;

public abstract class GrimACFabricLoaderPlugin implements PlatformLoader {
    public static MinecraftServer FABRIC_SERVER;
    public static GrimACFabricLoaderPlugin LOADER;

    protected final LazyHolder<FabricPlatformScheduler> scheduler = LazyHolder.simple(FabricPlatformScheduler::new);
    // Since we JiJ PacketEvents and depend on it on Fabric, we can always just get the API instance since it loads firsts
    protected final PacketEventsAPI<?> packetEvents = PacketEvents.getAPI();
    protected final LazyHolder<FabricSenderFactory> senderFactory = LazyHolder.simple(FabricSenderFactory::new);
    protected final LazyHolder<CommandManager<Sender>> commandManager = LazyHolder.simple(this::createCommandManager);
    protected final LazyHolder<ItemResetHandler> itemResetHandler = LazyHolder.simple(FabricItemResetHandler::new);
    protected final LazyHolder<GrimPlugin> plugin = LazyHolder.simple(() ->
            new BasicGrimPlugin(
                    JULoggerFactory.createLogger("GrimAC"),
                    new File(FabricLoader.getInstance().getConfigDir().toFile(), "GrimAC"),
                    FabricLoader.getInstance().getModContainer("grimac").get().getMetadata().getVersion().getFriendlyString(),
                    FabricLoader.getInstance().getModContainer("grimac").get().getMetadata().getDescription(),
                    FabricLoader.getInstance().getModContainer("grimac").get().getMetadata().getAuthors().stream().map(Person::getName).collect(Collectors.toList())
            )
    );
    protected final PlatformPluginManager platformPluginManager = new FabricPlatformPluginManager();
    protected final MessagePlaceHolderManager messagePlaceHolderManager = new FabricMessagePlaceHolderManager();
    protected final LazyHolder<FabricPermissionRegistrationManager> fabricPermissionRegistrationManager = LazyHolder.simple(FabricPermissionRegistrationManager::new);

    protected final ParserDescriptorFactory parserFactory;
    protected final FabricPlatformPlayerFactory playerFactory;
    protected final PlatformServer platformServer;
    @Getter
    protected final IFabricConversionUtil fabricConversionUtil;
    protected final IFabricMessageUtil fabricMessageUtil;

    public GrimACFabricLoaderPlugin(
                                    ParserDescriptorFactory parserDescriptorFactory,
                                    FabricPlatformPlayerFactory playerFactory,
                                    PlatformServer platformServer,
                                    IFabricMessageUtil fabricMessageUtil,
                                    IFabricConversionUtil fabricConversionUtil
    ) {
        this.parserFactory = parserDescriptorFactory;
        this.playerFactory = playerFactory;
        this.platformServer = platformServer;
        this.fabricMessageUtil = fabricMessageUtil;
        this.fabricConversionUtil = fabricConversionUtil;
    }

    @Override
    public FabricPlatformScheduler getScheduler() {
        return scheduler.get();
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
    public void registerAPIService() {
        GrimAPIProvider.init(GrimAPI.INSTANCE.getExternalAPI());
    }

    @Override @NotNull
    public  MessagePlaceHolderManager getMessagePlaceHolderManager() {
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

    @Override
    public ParserDescriptorFactory getParserDescriptorFactory() {
        return parserFactory;
    }

    @Override
    public FabricPlatformPlayerFactory getPlatformPlayerFactory() {
        return playerFactory;
    }

    @Override
    public PlatformServer getPlatformServer() {
        return platformServer;
    }

    public IFabricMessageUtil getFabricMessageUtils() {
        return fabricMessageUtil;
    }

    public abstract ServerVersion getNativeVersion();
}
