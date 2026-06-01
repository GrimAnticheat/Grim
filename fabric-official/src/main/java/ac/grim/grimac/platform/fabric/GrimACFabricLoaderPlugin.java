package ac.grim.grimac.platform.fabric;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.GrimAPIProvider;
import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.internal.plugin.resolver.GrimExtensionManager;
import ac.grim.grimac.platform.api.PlatformLoader;
import ac.grim.grimac.platform.api.command.CommandService;
import ac.grim.grimac.platform.api.manager.ItemResetHandler;
import ac.grim.grimac.platform.api.manager.MessagePlaceHolderManager;
import ac.grim.grimac.platform.api.manager.PermissionRegistrationManager;
import ac.grim.grimac.platform.api.manager.PlatformPluginManager;
import ac.grim.grimac.platform.api.permissions.PermissionDefaultValue;
import ac.grim.grimac.platform.api.sender.SenderFactory;
import ac.grim.grimac.platform.fabric.manager.FabricItemResetHandler;
import ac.grim.grimac.platform.fabric.manager.FabricMessagePlaceHolderManager;
import ac.grim.grimac.platform.fabric.manager.FabricPlatformPluginManager;
import ac.grim.grimac.platform.fabric.player.FabricPlatformPlayerFactory;
import ac.grim.grimac.platform.fabric.resolver.FabricResolverRegistrar;
import ac.grim.grimac.platform.fabric.scheduler.FabricPlatformScheduler;
import ac.grim.grimac.platform.fabric.sender.NoopFabricSenderFactory;
import ac.grim.grimac.platform.fabric.utils.convert.IFabricConversionUtil;
import ac.grim.grimac.platform.fabric.utils.message.IFabricMessageUtil;
import ac.grim.grimac.utils.lazy.LazyHolder;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import lombok.Getter;
import net.minecraft.server.MinecraftServer;

// fabric-official variant — mirrors the intermediary loader minus APIs that don't
// link against 26.X: cloud-fabric (no-op CommandService) and fabric-permissions-api
// (no-op PermissionRegistrationManager + op-level fallback).
public abstract class GrimACFabricLoaderPlugin implements PlatformLoader {
    public static MinecraftServer FABRIC_SERVER;
    public static GrimACFabricLoaderPlugin LOADER;

    protected final LazyHolder<FabricPlatformScheduler> scheduler = LazyHolder.simple(FabricPlatformScheduler::new);
    protected final PacketEventsAPI<?> packetEvents = PacketEvents.getAPI();
    protected final LazyHolder<NoopFabricSenderFactory> senderFactory = LazyHolder.simple(NoopFabricSenderFactory::new);
    protected final LazyHolder<ItemResetHandler> itemResetHandler = LazyHolder.simple(FabricItemResetHandler::new);
    protected final GrimPlugin plugin;
    @Getter
    protected final PlatformPluginManager pluginManager = new FabricPlatformPluginManager();
    @Getter
    protected final MessagePlaceHolderManager messagePlaceHolderManager = new FabricMessagePlaceHolderManager();

    protected final FabricPlatformPlayerFactory playerFactory;
    protected final AbstractFabricPlatformServer platformServer;
    @Getter
    protected final IFabricConversionUtil fabricConversionUtil;
    protected final IFabricMessageUtil fabricMessageUtil;

    public GrimACFabricLoaderPlugin(
            FabricPlatformPlayerFactory playerFactory,
            AbstractFabricPlatformServer platformServer,
            IFabricMessageUtil fabricMessageUtil,
            IFabricConversionUtil fabricConversionUtil
    ) {
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
    public SenderFactory<?> getSenderFactory() {
        return senderFactory.get();
    }

    @Override
    public CommandService getCommandService() {
        return () -> {}; // No commands on 26.X — cloud-fabric not yet ported.
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
        return (name, defaultValue) -> {}; // No-op; fabric-permissions-api not ported.
    }

    public NoopFabricSenderFactory getFabricSenderFactory() {
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
