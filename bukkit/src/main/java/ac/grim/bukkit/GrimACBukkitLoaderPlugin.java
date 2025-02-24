package ac.grim.bukkit;

import ac.grim.bukkit.initables.BukkitEventManager;
import ac.grim.bukkit.initables.BukkitExemptOnlinePlayersOnReload;
import ac.grim.bukkit.initables.BukkitTickEndEvent;
import ac.grim.bukkit.manager.BukkitMessagePlaceHolderManager;
import ac.grim.bukkit.manager.BukkitParserDescriptorFactory;
import ac.grim.bukkit.manager.BukkitPlatformPluginManager;
import ac.grim.bukkit.player.BukkitPlatformPlayerFactory;
import ac.grim.bukkit.sender.BukkitSenderFactory;
import ac.grim.bukkit.manager.BukkitItemResetHandler;
import ac.grim.bukkit.utils.placeholder.PlaceholderAPIExpansion;
import ac.grim.bukkit.utils.scheduler.bukkit.BukkitPlatformScheduler;
import ac.grim.bukkit.utils.scheduler.folia.FoliaPlatformScheduler;
import ac.grim.grimac.BasicGrimPlugin;
import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.GrimAbstractAPI;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.platform.api.PlatformServer;
import ac.grim.grimac.platform.api.manager.MessagePlaceHolderManager;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.lazy.LazyHolder;
import ac.grim.grimac.platform.api.PlatformLoader;
import ac.grim.grimac.platform.api.manager.ItemResetHandler;
import ac.grim.grimac.platform.api.manager.ParserDescriptorFactory;
import ac.grim.grimac.platform.api.manager.PlatformPluginManager;
import ac.grim.grimac.platform.api.player.PlatformPlayerFactory;
import ac.grim.grimac.platform.api.scheduler.PlatformScheduler;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.api.sender.SenderFactory;
import com.github.retrooper.packetevents.PacketEventsAPI;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import ac.grim.grimac.api.GrimPlugin;
import org.jetbrains.annotations.NotNull;

public final class GrimACBukkitLoaderPlugin extends JavaPlugin implements PlatformLoader {

    public static GrimACBukkitLoaderPlugin PLUGIN;

    private final LazyHolder<PlatformScheduler> scheduler = LazyHolder.simple(this::createScheduler);
    private final LazyHolder<PacketEventsAPI<?>> packetEvents = LazyHolder.simple(() -> SpigotPacketEventsBuilder.build(this));
    private final LazyHolder<BukkitSenderFactory> senderFactory = LazyHolder.simple(BukkitSenderFactory::new);
    private final LazyHolder<CommandManager<Sender>> commandManager = LazyHolder.simple(this::createCommandManager);
    private final LazyHolder<ItemResetHandler> itemResetHandler = LazyHolder.simple(BukkitItemResetHandler::new);

    private final PlatformPlayerFactory playerFactory = new BukkitPlatformPlayerFactory();
    private final ParserDescriptorFactory parserFactory = new BukkitParserDescriptorFactory();
    private final PlatformPluginManager platformPluginManager = new BukkitPlatformPluginManager();
    private final GrimPlugin plugin;
    private final PlatformServer platformServer = new BukkitPlatformServer();
    private final MessagePlaceHolderManager messagePlaceHolderManager = new BukkitMessagePlaceHolderManager();

    public GrimACBukkitLoaderPlugin() {
        this.plugin = new BasicGrimPlugin(
                this.getLogger(),
                this.getDataFolder(),
                this.getDescription().getVersion(),
                this.getDescription().getDescription(),
                this.getDescription().getAuthors()
        );
    }

    @Override
    public void onLoad() {
        PLUGIN = this;
        GrimAPI.INSTANCE.load(this, this.getBukkitInitTasks());
    }

    private Initable[] getBukkitInitTasks() {
        return new Initable[]{
                new BukkitExemptOnlinePlayersOnReload(),
                new BukkitEventManager(),
                new BukkitTickEndEvent(),
                () -> {
                    if (MessageUtil.hasPlaceholderAPI) {
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
    public PlatformPlayerFactory getPlatformPlayerFactory() {
        return playerFactory;
    }

    @Override
    public ParserDescriptorFactory getParserDescriptorFactory() {
        return parserFactory;
    }

    @Override
    public PacketEventsAPI<?> getPacketEvents() {
        return packetEvents.get();
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
    public SenderFactory<CommandSender> getSenderFactory() {
        return senderFactory.get();
    }

    @Override
    public GrimPlugin getPlugin() {
        return plugin;
    }

    @Override
    public PlatformPluginManager getPluginManager() {
        return platformPluginManager;
    }

    @Override
    public PlatformServer getPlatformServer() {
        return platformServer;
    }

    @Override
    public void registerAPIService() {
        Bukkit.getServicesManager().register(GrimAbstractAPI.class, GrimAPI.INSTANCE.getExternalAPI(), GrimACBukkitLoaderPlugin.PLUGIN, ServicePriority.Normal);
    }

    @Override
    public @NotNull MessagePlaceHolderManager getMessagePlaceHolderManager() {
        return messagePlaceHolderManager;
    }

    private PlatformScheduler createScheduler() {
        return GrimAPI.PLATFORM == GrimAPI.Platform.FOLIA ? new FoliaPlatformScheduler() : new BukkitPlatformScheduler();
    }

    private CommandManager<Sender> createCommandManager() {
        LegacyPaperCommandManager<Sender> manager = new LegacyPaperCommandManager<>(
                this,
                ExecutionCoordinator.simpleCoordinator(),
                senderFactory.get()
        );
        if (manager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            manager.registerBrigadier();
        } else if (manager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            manager.registerAsynchronousCompletions();
        }
        return manager;
    }

    public BukkitSenderFactory getBukkitSenderFactory() {
        return PLUGIN.senderFactory.get();
    }
}
