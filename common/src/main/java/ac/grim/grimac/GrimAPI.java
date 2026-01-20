package ac.grim.grimac;

import ac.grim.grimac.api.event.EventBus;
import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.internal.plugin.resolver.GrimExtensionManager;
import ac.grim.grimac.internal.event.OptimizedEventBus;
import ac.grim.grimac.manager.AlertManagerImpl;
import ac.grim.grimac.manager.DiscordManager;
import ac.grim.grimac.manager.InitManager;
import ac.grim.grimac.manager.SpectateManager;
import ac.grim.grimac.manager.TickManager;
import ac.grim.grimac.manager.config.BaseConfigManager;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.manager.violationdatabase.ViolationDatabaseManager;
import ac.grim.grimac.platform.api.Platform;
import ac.grim.grimac.platform.api.PlatformLoader;
import ac.grim.grimac.platform.api.PlatformServer;
import ac.grim.grimac.platform.api.command.CommandService;
import ac.grim.grimac.platform.api.manager.ItemResetHandler;
import ac.grim.grimac.platform.api.manager.MessagePlaceHolderManager;
import ac.grim.grimac.platform.api.manager.PermissionRegistrationManager;
import ac.grim.grimac.platform.api.manager.PlatformPluginManager;
import ac.grim.grimac.platform.api.player.PlatformPlayerFactory;
import ac.grim.grimac.platform.api.scheduler.PlatformScheduler;
import ac.grim.grimac.platform.api.sender.SenderFactory;
import ac.grim.grimac.utils.anticheat.PlayerDataManager;
import ac.grim.grimac.utils.common.arguments.CommonGrimArguments;
import ac.grim.grimac.utils.reflection.ReflectionUtils;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;


@Getter
public final class GrimAPI {
    public static final GrimAPI INSTANCE = new GrimAPI();

    @Getter
    private final Platform platform = detectPlatform();
    private final BaseConfigManager configManager;
    private final AlertManagerImpl alertManager;
    private final SpectateManager spectateManager;
    private final DiscordManager discordManager;
    private final PlayerDataManager playerDataManager;
    private final TickManager tickManager;
    private final GrimExtensionManager extensionManager;
    private final EventBus eventBus;
    private final GrimExternalAPI externalAPI;
    private ViolationDatabaseManager violationDatabaseManager;
    private PlatformLoader loader;
    @Getter
    private InitManager initManager;
    private boolean initialized = false;

    private GrimAPI() {
        this.configManager = new BaseConfigManager();
        this.alertManager = new AlertManagerImpl();
        this.spectateManager = new SpectateManager();
        this.discordManager = new DiscordManager();
        this.playerDataManager = new PlayerDataManager();
        this.tickManager = new TickManager();
        this.extensionManager = new GrimExtensionManager();
        this.eventBus = new OptimizedEventBus(extensionManager);
        this.externalAPI = new GrimExternalAPI(this);
    }

    // the order matters
    private static Platform detectPlatform() {
        Platform override = CommonGrimArguments.PLATFORM_OVERRIDE.value();
        if (override != null) return override;
        if (ReflectionUtils.hasClass("io.papermc.paper.threadedregions.RegionizedServer")) return Platform.FOLIA;
        if (ReflectionUtils.hasClass("org.bukkit.Bukkit")) return Platform.BUKKIT;
        if (ReflectionUtils.hasClass("net.fabricmc.loader.api.FabricLoader")) return Platform.FABRIC;
        throw new IllegalStateException("Unknown platform!");
    }

    public void load(PlatformLoader platformLoader, Initable... platformSpecificInitables) {
        this.loader = platformLoader;
        this.violationDatabaseManager = new ViolationDatabaseManager(getGrimPlugin());
        this.initManager = new InitManager(loader.getPacketEvents(), platformSpecificInitables);
        this.initManager.load();
        this.initialized = true;
    }

    public void start() {
        checkInitialized();
        initManager.start();
    }

    public void stop() {
        checkInitialized();
        initManager.stop();
    }

    public PlatformScheduler getScheduler() {
        return loader.getScheduler();
    }

    public PlatformPlayerFactory getPlatformPlayerFactory() {
        return loader.getPlatformPlayerFactory();
    }

    public GrimPlugin getGrimPlugin() {
        return loader.getPlugin();
    }

    public SenderFactory<?> getSenderFactory() {
        return loader.getSenderFactory();
    }

    public ItemResetHandler getItemResetHandler() {
        return loader.getItemResetHandler();
    }

    public PlatformPluginManager getPluginManager() {
        return loader.getPluginManager();
    }

    public PlatformServer getPlatformServer() {
        return loader.getPlatformServer();
    }

    public @NotNull MessagePlaceHolderManager getMessagePlaceHolderManager() {
        return loader.getMessagePlaceHolderManager();
    }

    public CommandService getCommandService() {
        return loader.getCommandService();
    }

    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("GrimAPI has not been initialized!");
        }
    }

    public PermissionRegistrationManager getPermissionManager() {
        return loader.getPermissionManager();
    }

    public GrimExtensionManager getExtensionManager() {
        return extensionManager;
    }
}
