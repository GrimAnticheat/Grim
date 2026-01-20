package ac.grim.grimac.platform.bukkit;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.GrimExternalAPI;
import ac.grim.grimac.api.GrimAPIProvider;
import ac.grim.grimac.api.GrimAbstractAPI;
import ac.grim.grimac.api.event.EventBus;
import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.command.CloudCommandService;
import ac.grim.grimac.internal.platform.bukkit.resolver.BukkitResolverRegistrar;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.manager.init.start.ExemptOnlinePlayersOnReload;
import ac.grim.grimac.manager.init.start.StartableInitable;
import ac.grim.grimac.platform.api.Platform;
import ac.grim.grimac.platform.api.PlatformLoader;
import ac.grim.grimac.platform.api.PlatformServer;
import ac.grim.grimac.platform.api.command.CommandService;
import ac.grim.grimac.platform.api.manager.ItemResetHandler;
import ac.grim.grimac.platform.api.manager.MessagePlaceHolderManager;
import ac.grim.grimac.platform.api.manager.PlatformPluginManager;
import ac.grim.grimac.platform.api.manager.cloud.CloudCommandAdapter;
import ac.grim.grimac.platform.api.player.PlatformPlayerFactory;
import ac.grim.grimac.platform.api.scheduler.PlatformScheduler;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.api.sender.SenderFactory;
import ac.grim.grimac.platform.bukkit.initables.BukkitBStats;
import ac.grim.grimac.platform.bukkit.initables.BukkitEventManager;
import ac.grim.grimac.platform.bukkit.initables.BukkitTickEndEvent;
import ac.grim.grimac.platform.bukkit.manager.BukkitItemResetHandler;
import ac.grim.grimac.platform.bukkit.manager.BukkitMessagePlaceHolderManager;
import ac.grim.grimac.platform.bukkit.manager.BukkitParserDescriptorFactory;
import ac.grim.grimac.platform.bukkit.manager.BukkitPermissionRegistrationManager;
import ac.grim.grimac.platform.bukkit.manager.BukkitPlatformPluginManager;
import ac.grim.grimac.platform.bukkit.player.BukkitPlatformPlayerFactory;
import ac.grim.grimac.platform.bukkit.scheduler.bukkit.BukkitPlatformScheduler;
import ac.grim.grimac.platform.bukkit.scheduler.folia.FoliaPlatformScheduler;
import ac.grim.grimac.platform.bukkit.sender.BukkitSenderFactory;
import ac.grim.grimac.platform.bukkit.utils.placeholder.PlaceholderAPIExpansion;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.lazy.LazyHolder;
import com.github.retrooper.packetevents.PacketEventsAPI;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.brigadier.BrigadierSetting;
import org.incendo.cloud.brigadier.CloudBrigadierManager;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;


public final class GrimACBukkitLoaderPlugin extends JavaPlugin implements PlatformLoader {

    public static GrimACBukkitLoaderPlugin LOADER;

    private final LazyHolder<PlatformScheduler> scheduler = LazyHolder.simple(this::createScheduler);
    private final LazyHolder<PacketEventsAPI<?>> packetEvents = LazyHolder.simple(() -> SpigotPacketEventsBuilder.build(this));
    private final LazyHolder<BukkitSenderFactory> senderFactory = LazyHolder.simple(BukkitSenderFactory::new);
    private final LazyHolder<ItemResetHandler> itemResetHandler = LazyHolder.simple(BukkitItemResetHandler::new);
    private final LazyHolder<CommandService> commandService = LazyHolder.simple(this::createCommandService);
    private final CloudCommandAdapter commandAdapter = new BukkitParserDescriptorFactory();

    @Getter private final PlatformPlayerFactory platformPlayerFactory = new BukkitPlatformPlayerFactory();
    @Getter private final PlatformPluginManager pluginManager = new BukkitPlatformPluginManager();
    @Getter private final GrimPlugin plugin;
    @Getter private final PlatformServer platformServer = new BukkitPlatformServer();
    @Getter private final MessagePlaceHolderManager messagePlaceHolderManager = new BukkitMessagePlaceHolderManager();
    @Getter private final BukkitPermissionRegistrationManager permissionManager = new BukkitPermissionRegistrationManager();

    public GrimACBukkitLoaderPlugin() {
        BukkitResolverRegistrar registrar = new BukkitResolverRegistrar();
        registrar.registerAll(GrimAPI.INSTANCE.getExtensionManager());
        this.plugin = registrar.resolvePlugin(this);
    }

    @Override
    public void onLoad() {
        LOADER = this;
        GrimAPI.INSTANCE.load(this, this.getBukkitInitTasks());
    }

    private Initable[] getBukkitInitTasks() {
        return new Initable[] {
                new ExemptOnlinePlayersOnReload(),
                new BukkitEventManager(),
                new BukkitTickEndEvent(),
                new BukkitBStats(),
                (StartableInitable) () -> {
                    if (BukkitMessagePlaceHolderManager.hasPlaceholderAPI) {
                        new PlaceholderAPIExpansion().register();
                    }
                }
        };
    }

    @Override
    public void onEnable() {
        GrimAPI.INSTANCE.start();
    }

    @Override
    public void onDisable() {
        GrimAPI.INSTANCE.stop();
    }

    @Override
    public PlatformScheduler getScheduler() {
        return scheduler.get();
    }

    @Override
    public PacketEventsAPI<?> getPacketEvents() {
        return packetEvents.get();
    }

    @Override
    public ItemResetHandler getItemResetHandler() {
        return itemResetHandler.get();
    }

    @Override
    public CommandService getCommandService() {
        return commandService.get();
    }

    @Override
    public SenderFactory<CommandSender> getSenderFactory() {
        return senderFactory.get();
    }

    @Override
    public void registerAPIService() {
        final GrimExternalAPI externalAPI = GrimAPI.INSTANCE.getExternalAPI();
        final EventBus eventBus = externalAPI.getEventBus();
        final ac.grim.grimac.api.plugin.GrimPlugin context = GrimAPI.INSTANCE.getGrimPlugin();

        eventBus.subscribe(context, ac.grim.grimac.api.event.events.GrimJoinEvent.class, (event) -> {
            ac.grim.grimac.api.events.GrimJoinEvent bukkitEvent =
                    new ac.grim.grimac.api.events.GrimJoinEvent(event.getUser());

            Bukkit.getPluginManager().callEvent(bukkitEvent);
        });

        eventBus.subscribe(context, ac.grim.grimac.api.event.events.GrimQuitEvent.class, (event) -> {
            ac.grim.grimac.api.events.GrimQuitEvent bukkitEvent =
                    new ac.grim.grimac.api.events.GrimQuitEvent(event.getUser());

            Bukkit.getPluginManager().callEvent(bukkitEvent);
        });

        eventBus.subscribe(context, ac.grim.grimac.api.event.events.GrimReloadEvent.class, (event) -> {
            ac.grim.grimac.api.events.GrimReloadEvent bukkitEvent =
                    new ac.grim.grimac.api.events.GrimReloadEvent(event.isSuccess());

            Bukkit.getPluginManager().callEvent(bukkitEvent);
        });

        eventBus.subscribe(context, ac.grim.grimac.api.event.events.FlagEvent.class, (event) -> {
            ac.grim.grimac.api.events.FlagEvent bukkitEvent =
                    new ac.grim.grimac.api.events.FlagEvent(
                            event.getUser(),
                            event.getCheck(),
                            event.getVerbose()
                    );

            Bukkit.getPluginManager().callEvent(bukkitEvent);

            if (bukkitEvent.isCancelled()) {
                event.setCancelled(true);
            }
        });

        eventBus.subscribe(context, ac.grim.grimac.api.event.events.CommandExecuteEvent.class, (event) -> {
            ac.grim.grimac.api.events.CommandExecuteEvent bukkitEvent =
                    new ac.grim.grimac.api.events.CommandExecuteEvent(
                            event.getUser(),
                            event.getCheck(),
                            event.getVerbose(),
                            event.getCommand()
                    );

            Bukkit.getPluginManager().callEvent(bukkitEvent);

            if (bukkitEvent.isCancelled()) {
                event.setCancelled(true);
            }
        });

        eventBus.subscribe(context, ac.grim.grimac.api.event.events.CompletePredictionEvent.class, (event) -> {
            // Note: New event doesn't have verbose, passing null or check name is standard fallback
            ac.grim.grimac.api.events.CompletePredictionEvent bukkitEvent =
                    new ac.grim.grimac.api.events.CompletePredictionEvent(
                            event.getUser(),
                            event.getCheck(),
                            "",
                            event.getOffset()
                    );

            Bukkit.getPluginManager().callEvent(bukkitEvent);

            if (bukkitEvent.isCancelled()) {
                event.setCancelled(true);
            }
        });

        GrimAPIProvider.init(externalAPI);
        Bukkit.getServicesManager().register(GrimAbstractAPI.class, externalAPI, GrimACBukkitLoaderPlugin.LOADER, ServicePriority.Normal);
    }

    private PlatformScheduler createScheduler() {
        return GrimAPI.INSTANCE.getPlatform() == Platform.FOLIA ? new FoliaPlatformScheduler() : new BukkitPlatformScheduler();
    }

    private CommandService createCommandService() {
        try {
            return new CloudCommandService(this::createCloudCommandManager, commandAdapter);
        } catch (Throwable t) {
            LogUtil.warn("CRITICAL: Failed to initialize Command Framework. " +
                    "Grim will continue to run with no commands.", t);
            return () -> {};
        }
    }

    private CommandManager<Sender> createCloudCommandManager() {
        LegacyPaperCommandManager<Sender> manager = new LegacyPaperCommandManager<>(
                this,
                ExecutionCoordinator.simpleCoordinator(),
                senderFactory.get()
        );
        if (manager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            try {
                manager.registerBrigadier();
                CloudBrigadierManager<Sender, ?> cbm = manager.brigadierManager();
                cbm.settings().set(BrigadierSetting.FORCE_EXECUTABLE, true);
            } catch (Throwable t) {
                LogUtil.error("Failed to register Brigadier native completions. Falling back to standard completions.", t);
            }
        } else if (manager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            manager.registerAsynchronousCompletions();
        }
        return manager;
    }

    public BukkitSenderFactory getBukkitSenderFactory() {
        return LOADER.senderFactory.get();
    }
}
