package ac.grim.grimac;

import ac.grim.grimac.api.GrimPlugin;
import ac.grim.grimac.manager.*;
import ac.grim.grimac.manager.config.BaseConfigManager;
import ac.grim.grimac.platform.api.manager.ItemResetHandler;
import ac.grim.grimac.platform.api.manager.ParserDescriptorFactory;
import ac.grim.grimac.platform.api.player.PlatformPlayerFactory;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.api.sender.SenderFactory;
import ac.grim.grimac.utils.anticheat.PlayerDataManager;
import ac.grim.grimac.platform.api.scheduler.PlatformScheduler;
import com.github.retrooper.packetevents.PacketEventsAPI;
import lombok.Getter;
import org.incendo.cloud.CommandManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Getter
public final class GrimAPI {
    public static final GrimAPI INSTANCE = new GrimAPI();
    public static final Platform PLATFORM = detectPlatform();

    private final BaseConfigManager configManager;
    private final AlertManagerImpl alertManager;
    private final SpectateManager spectateManager;
    private final DiscordManager discordManager;
    private final PlayerDataManager playerDataManager;
    private final TickManager tickManager;
    private final GrimExternalAPI externalAPI;
    private final PlatformDependentComponents components;

    // Private constructor for INSTANCE
    private GrimAPI() {
        this.configManager = new BaseConfigManager();
        this.alertManager = new AlertManagerImpl();
        this.spectateManager = new SpectateManager();
        this.discordManager = new DiscordManager();
        this.playerDataManager = new PlayerDataManager();
        this.tickManager = new TickManager();
        this.externalAPI = new GrimExternalAPI(this);
        this.components = new PlatformDependentComponents();
    }

    // Holder class for platform-dependent components (excluding ItemResetHandler)
    @Getter
    private static final class PlatformDependentComponents {
        private final PlatformScheduler scheduler;
        private final PlatformPlayerFactory platformPlayerFactory;
        private final InitManager initManager;
        private final GrimPlugin plugin;
        private final ParserDescriptorFactory parserDescriptorFactory;

        // Constructor for uninitialized state
        private PlatformDependentComponents() {
            this.platformPlayerFactory = null;
            this.scheduler = null;
            this.initManager = null;
            this.plugin = null;
            this.parserDescriptorFactory = null;
        }

        // Constructor for initialized state
        private PlatformDependentComponents(GrimPlugin plugin, PlatformScheduler scheduler, PlatformPlayerFactory platformPlayerFactory,
                                            PacketEventsAPI<?> packetEventsAPI,
                                            Supplier<CommandManager<Sender>> commandManagerSupplier,
                                            ParserDescriptorFactory parserDescriptorFactory) {
            this.plugin = plugin;
            this.scheduler = scheduler;
            this.platformPlayerFactory = platformPlayerFactory;
            this.initManager = new InitManager(packetEventsAPI, commandManagerSupplier);
            this.parserDescriptorFactory = parserDescriptorFactory;
        }

        private boolean isInitialized() {
            return plugin != null;
        }
    }

    // Holder for ItemResetHandler (dependent on InitManager)
    @Getter
    private static final class ItemResetHandlerHolder {
        private final ItemResetHandler itemResetHandler;

        private ItemResetHandlerHolder(ItemResetHandler itemResetHandler) {
            this.itemResetHandler = itemResetHandler;
        }
    }

    // Lazy initialization holders
    private static final class InitializedComponentsHolder {
        private static volatile PlatformDependentComponents initializedComponents = null;
        private static volatile ItemResetHandlerHolder itemResetHandlerHolder = null;

        private static PlatformDependentComponents getOrCreateComponents(GrimPlugin plugin,
                                                                         PlatformScheduler scheduler,
                                                                         PlatformPlayerFactory platformPlayerFactory,
                                                                         PacketEventsAPI<?> packetEventsAPI,
                                                                         Supplier<CommandManager<Sender>> commandManagerSupplier,
                                                                         ParserDescriptorFactory parserDescriptorFactory) {
            if (initializedComponents == null) {
                synchronized (InitializedComponentsHolder.class) {
                    if (initializedComponents == null) {
                        initializedComponents = new PlatformDependentComponents(plugin, scheduler, platformPlayerFactory, packetEventsAPI, commandManagerSupplier, parserDescriptorFactory);
                        initializedComponents.getInitManager().load();
                    }
                }
            }
            return initializedComponents;
        }

        private static ItemResetHandlerHolder getOrCreateItemResetHandler(Supplier<ItemResetHandler> itemResetHandlerFactory) {
            if (itemResetHandlerHolder == null) {
                synchronized (InitializedComponentsHolder.class) {
                    if (itemResetHandlerHolder == null) {
                        // Create ItemResetHandler after initManager.load() has run
                        ItemResetHandler itemResetHandler = itemResetHandlerFactory.get();
                        itemResetHandlerHolder = new ItemResetHandlerHolder(itemResetHandler);
                    }
                }
            }
            return itemResetHandlerHolder;
        }
    }

    public void load(GrimPlugin grimPlugin,
                     PlatformScheduler platformScheduler,
                     PlatformPlayerFactory platformPlayerFactory,
                     PacketEventsAPI<?> packetEventsAPI,
                     ParserDescriptorFactory parserDescriptorFactory,
                     Supplier<CommandManager<Sender>> commandManager,
                     Supplier<ItemResetHandler> itemResetHandlerFactory
    ) {
        // Initialize platform-dependent components (excluding ItemResetHandler)
        PlatformDependentComponents newComponents = InitializedComponentsHolder.getOrCreateComponents(
                grimPlugin, platformScheduler, platformPlayerFactory, packetEventsAPI, commandManager, parserDescriptorFactory
        );

        if (newComponents == INSTANCE.components) {
            throw new IllegalStateException("GrimAPI has already been initialized!");
        }

        // Initialize ItemResetHandler after InitManager.load() has run
        ItemResetHandlerHolder newItemResetHandlerHolder = InitializedComponentsHolder.getOrCreateItemResetHandler(itemResetHandlerFactory);

        if (newItemResetHandlerHolder == null) {
            throw new IllegalStateException("Failed to initialize ItemResetHandler!");
        }
    }

    public void start() {
        checkInitialized();
        getInitManager().start();
    }

    public void stop() {
        checkInitialized();
        getInitManager().stop();
    }

    // Delegate methods for platform-dependent components
    public PlatformScheduler getScheduler() {
        return getComponents().getScheduler();
    }

    public PlatformPlayerFactory getPlatformPlayerFactory() {
        return getComponents().getPlatformPlayerFactory();
    }

    public ParserDescriptorFactory getParserDescriptors() {
        return getComponents().getParserDescriptorFactory();
    }

    public InitManager getInitManager() {
        return getComponents().getInitManager();
    }

    public GrimPlugin getPlugin() {
        return getComponents().getPlugin();
    }

    public ItemResetHandler getItemResetHandler() {
        ItemResetHandlerHolder holder = InitializedComponentsHolder.itemResetHandlerHolder;
        if (holder == null) {
            return null; // Uninitialized state
        }
        return holder.getItemResetHandler();
    }

    private PlatformDependentComponents getComponents() {
        PlatformDependentComponents initialized = InitializedComponentsHolder.initializedComponents;
        return initialized != null ? initialized : INSTANCE.components;
    }

    private void checkInitialized() {
        if (!getComponents().isInitialized() || InitializedComponentsHolder.itemResetHandlerHolder == null) {
            throw new IllegalStateException("GrimAPI has not been initialized!");
        }
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static Platform detectPlatform() {
        final Map<String, Platform> platforms = Collections.unmodifiableMap(new HashMap<>() {{
            put("io.papermc.paper.threadedregions.RegionizedServer", Platform.FOLIA);
            put("org.bukkit.Bukkit", Platform.BUKKIT);
            put("net.fabricmc.loader.api.FabricLoader", Platform.FABRIC);
        }});

        return platforms.entrySet().stream()
                .filter(entry -> isClassPresent(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unknown platform!"));
    }

    public enum Platform {
        FABRIC,
        BUKKIT,
        FOLIA
    }
}
