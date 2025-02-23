package ac.grim.bukkit;

import ac.grim.bukkit.command.parsers.BukkitParserDescriptorFactory;
import ac.grim.bukkit.player.BukkitPlatformPlayerFactory;
import ac.grim.bukkit.sender.BukkitSenderFactory;
import ac.grim.bukkit.utils.nms.BukkitNMS;
import ac.grim.bukkit.utils.scheduler.bukkit.BukkitPlatformScheduler;
import ac.grim.bukkit.utils.scheduler.folia.FoliaPlatformScheduler;
import ac.grim.grimac.BasicGrimPlugin;
import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.api.LazyHolder;
import ac.grim.grimac.platform.api.PlatformLoader;
import ac.grim.grimac.platform.api.manager.ItemResetHandler;
import ac.grim.grimac.platform.api.manager.ParserDescriptorFactory;
import ac.grim.grimac.platform.api.player.PlatformPlayerFactory;
import ac.grim.grimac.platform.api.scheduler.PlatformScheduler;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.api.sender.SenderFactory;
import com.github.retrooper.packetevents.PacketEventsAPI;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import ac.grim.grimac.api.GrimPlugin;

public final class GrimACBukkitLoaderPlugin extends JavaPlugin implements PlatformLoader {

    public static GrimACBukkitLoaderPlugin PLUGIN;

    private final LazyHolder<PlatformScheduler> scheduler = LazyHolder.of(this::createScheduler);
    private final LazyHolder<PlatformPlayerFactory> playerFactory = LazyHolder.of(BukkitPlatformPlayerFactory::new);
    private final LazyHolder<ParserDescriptorFactory> parserFactory = LazyHolder.of(BukkitParserDescriptorFactory::new);
    private final LazyHolder<PacketEventsAPI<?>> packetEvents = LazyHolder.of(() -> SpigotPacketEventsBuilder.build(this));
    private final LazyHolder<BukkitSenderFactory> senderFactory = LazyHolder.of(BukkitSenderFactory::new);
    private final LazyHolder<CommandManager<Sender>> commandManager = LazyHolder.of(this::createCommandManager);
    private final LazyHolder<ItemResetHandler> itemResetHandler = LazyHolder.of(BukkitNMS::new);
    private final LazyHolder<GrimPlugin> plugin = LazyHolder.of(this::createPlugin);

    @Override
    public void onLoad() {
        PLUGIN = this;
        GrimAPI.INSTANCE.load(this);
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
        return playerFactory.get();
    }

    @Override
    public ParserDescriptorFactory getParserDescriptorFactory() {
        return parserFactory.get();
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
        return plugin.get();
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

    private GrimPlugin createPlugin() {
        return new BasicGrimPlugin(
                this.getLogger(),
                this.getDataFolder(),
                this.getDescription().getVersion(),
                this.getDescription().getDescription(),
                this.getDescription().getAuthors()
        );
    }
}
