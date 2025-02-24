package ac.grim.grimac;

import ac.grim.grimac.api.GrimPlugin;
import ac.grim.grimac.manager.*;
import ac.grim.grimac.manager.config.BaseConfigManager;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.platform.api.PlatformLoader;
import ac.grim.grimac.platform.api.PlatformServer;
import ac.grim.grimac.platform.api.manager.ItemResetHandler;
import ac.grim.grimac.platform.api.manager.MessagePlaceHolderManager;
import ac.grim.grimac.platform.api.manager.ParserDescriptorFactory;
import ac.grim.grimac.platform.api.manager.PlatformPluginManager;
import ac.grim.grimac.platform.api.player.PlatformPlayerFactory;
import ac.grim.grimac.platform.api.scheduler.PlatformScheduler;
import ac.grim.grimac.platform.api.sender.SenderFactory;
import ac.grim.grimac.utils.anticheat.PlayerDataManager;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
    private PlatformLoader loader;
    private InitManager initManager;
    private boolean initialized = false;

    private GrimAPI() {
        this.configManager = new BaseConfigManager();
        this.alertManager = new AlertManagerImpl();
        this.spectateManager = new SpectateManager();
        this.discordManager = new DiscordManager();
        this.playerDataManager = new PlayerDataManager();
        this.tickManager = new TickManager();
        this.externalAPI = new GrimExternalAPI(this);
    }

    public void load(PlatformLoader platformLoader, Initable... platformSpecificInitables) {
        if (initialized) {
            throw new IllegalStateException("GrimAPI has already been initialized!");
        }

        this.loader = platformLoader;
        this.initManager = new InitManager(loader.getPacketEvents(), loader::getCommandManager, platformSpecificInitables);
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

    public ParserDescriptorFactory getParserDescriptors() {
        return loader.getParserDescriptorFactory();
    }

    public InitManager getInitManager() {
        return initManager;
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

    private void checkInitialized() {
        if (!initialized) {
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
