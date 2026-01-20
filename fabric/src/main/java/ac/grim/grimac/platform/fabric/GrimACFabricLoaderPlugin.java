package ac.grim.grimac.platform.fabric;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.GrimAPIProvider;
import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.command.CloudCommandService;
import ac.grim.grimac.internal.plugin.resolver.GrimExtensionManager;
import ac.grim.grimac.platform.api.PlatformLoader;
import ac.grim.grimac.platform.api.command.CommandService;
import ac.grim.grimac.platform.api.manager.*;
import ac.grim.grimac.platform.api.manager.cloud.CloudCommandAdapter;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.api.sender.SenderFactory;
import ac.grim.grimac.platform.fabric.manager.*;
import ac.grim.grimac.platform.fabric.player.FabricPlatformPlayerFactory;
import ac.grim.grimac.platform.fabric.resolver.FabricResolverRegistrar;
import ac.grim.grimac.platform.fabric.scheduler.FabricPlatformScheduler;
import ac.grim.grimac.platform.fabric.sender.FabricSenderFactory;
import ac.grim.grimac.platform.fabric.utils.convert.IFabricConversionUtil;
import ac.grim.grimac.platform.fabric.utils.message.IFabricMessageUtil;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.lazy.LazyHolder;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import lombok.Getter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.fabric.FabricServerCommandManager;
import org.jetbrains.annotations.NotNull;

public abstract class GrimACFabricLoaderPlugin implements PlatformLoader {
    public static MinecraftServer FABRIC_SERVER;
    public static GrimACFabricLoaderPlugin LOADER;

    protected final LazyHolder<FabricPlatformScheduler> scheduler = LazyHolder.simple(FabricPlatformScheduler::new);
    // Since we JiJ PacketEvents and depend on it on Fabric, we can always just get the API instance since it loads firsts
    protected final PacketEventsAPI<?> packetEvents = PacketEvents.getAPI();
    protected final LazyHolder<FabricSenderFactory> senderFactory = LazyHolder.simple(FabricSenderFactory::new);
    protected final LazyHolder<ItemResetHandler> itemResetHandler = LazyHolder.simple(FabricItemResetHandler::new);
    protected final LazyHolder<CommandService> commandService = LazyHolder.simple(this::createCommandService);
    protected final GrimPlugin plugin;
    @Getter
    protected final PlatformPluginManager pluginManager = new FabricPlatformPluginManager();
    @Getter
    protected final MessagePlaceHolderManager messagePlaceHolderManager = new FabricMessagePlaceHolderManager();
    protected final LazyHolder<FabricPermissionRegistrationManager> fabricPermissionRegistrationManager = LazyHolder.simple(FabricPermissionRegistrationManager::new);

    protected final LazyHolder<CommandAdapter> commandAdapter;
    protected final FabricPlatformPlayerFactory playerFactory;
    protected final AbstractFabricPlatformServer platformServer;
    @Getter
    protected final IFabricConversionUtil fabricConversionUtil;
    protected final IFabricMessageUtil fabricMessageUtil;

    public GrimACFabricLoaderPlugin(
            LazyHolder<CommandAdapter> parserDescriptorFactory,
            FabricPlatformPlayerFactory playerFactory,
            AbstractFabricPlatformServer platformServer,
            IFabricMessageUtil fabricMessageUtil,
            IFabricConversionUtil fabricConversionUtil
    ) {
        this.commandAdapter = parserDescriptorFactory;
        this.playerFactory = playerFactory;
        this.platformServer = platformServer;
        this.fabricMessageUtil = fabricMessageUtil;
        this.fabricConversionUtil = fabricConversionUtil;

        FabricResolverRegistrar resolverRegistrar = new FabricResolverRegistrar();
        GrimExtensionManager extensionManager = GrimAPI.INSTANCE.getExtensionManager();
        resolverRegistrar.registerAll(extensionManager);
        plugin = extensionManager.getPlugin("GrimAC");
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
    public ItemResetHandler getItemResetHandler() {
        return itemResetHandler.get();
    }

    @Override
    public SenderFactory<CommandSourceStack> getSenderFactory() {
        return senderFactory.get();
    }

    @Override
    public CommandService getCommandService() {
        return commandService.get();
    }

    @Override
    public GrimPlugin getPlugin() {
        return plugin;
    }

    @Override
    public void registerAPIService() {
        GrimAPIProvider.init(GrimAPI.INSTANCE.getExternalAPI());
    }

    @Override
    public PermissionRegistrationManager getPermissionManager() {
        return fabricPermissionRegistrationManager.get();
    }

    private CommandService createCommandService() {
        try {
            // Accessing CloudHelper triggers the JVM to load CloudCommandService and Cloud classes.
            // If the library is missing, this line throws NoClassDefFoundError immediately.
            return CloudHelper.create(senderFactory.get(), commandAdapter.get());
        } catch (Throwable t) {
            // Catches NoClassDefFoundError (Missing Lib) or other init crashes.
            LogUtil.warn("IMPORTANT: Command Framework failed to load (Missing Cloud Library?). \n" +
                    "Grim will run without commands enabled!");

            // Only spam stacktrace if it's weird, not if it's just missing.
            if (!(t instanceof NoClassDefFoundError)) {
                t.printStackTrace();
            }

            // Return No-Op to prevent NullPointers elsewhere
            return () -> {};
        }
    }

    private static class CloudHelper {
        static CommandService create(FabricSenderFactory factory, CommandAdapter commandAdapter) {
            SenderMapper<CommandSourceStack, Sender> mapper = SenderMapper.create(
                    factory::wrap,
                    factory::unwrap
            );
            CommandManager<@NotNull Sender> manager = new FabricServerCommandManager<>(
                    ExecutionCoordinator.simpleCoordinator(),
                    mapper
            );
            CloudCommandAdapter adapter = (CloudCommandAdapter) commandAdapter;
            return new CloudCommandService(() -> manager, adapter);
        }
    }

    public FabricSenderFactory getFabricSenderFactory() {
        return senderFactory.get();
    }

    @Override
    public FabricPlatformPlayerFactory getPlatformPlayerFactory() {
        return playerFactory;
    }

    @Override
    public AbstractFabricPlatformServer getPlatformServer() {
        return platformServer;
    }

    public IFabricMessageUtil getFabricMessageUtils() {
        return fabricMessageUtil;
    }

    public abstract ServerVersion getNativeVersion();
}
