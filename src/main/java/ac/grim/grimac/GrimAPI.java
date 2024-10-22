package ac.grim.grimac;

import ac.grim.grimac.manager.*;
import ac.grim.grimac.manager.config.BaseConfigManager;
import ac.grim.grimac.utils.anticheat.PlayerDataManager;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public enum GrimAPI {
    INSTANCE;

    private final BaseConfigManager configManager = new BaseConfigManager();
    private final AlertManagerImpl alertManager = new AlertManagerImpl();
    private final SpectateManager spectateManager = new SpectateManager();
    private final DiscordManager discordManager = new DiscordManager();
    private final PlayerDataManager playerDataManager = new PlayerDataManager();
    private final TickManager tickManager = new TickManager();
    private final GrimExternalAPI externalAPI = new GrimExternalAPI(this);
    private InitManager initManager;
    private JavaPlugin plugin;

    public void load(final JavaPlugin plugin) {
        this.plugin = plugin;
        initManager = new InitManager();
        initManager.load();
    }

    public void start(final JavaPlugin plugin) {
        this.plugin = plugin;
        initManager.start();
    }

    public void stop(final JavaPlugin plugin) {
        this.plugin = plugin;
        initManager.stop();
    }
}
