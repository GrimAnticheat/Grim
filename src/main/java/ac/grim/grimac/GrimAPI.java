package ac.grim.grimac;

import ac.grim.grimac.manager.*;
import ac.grim.grimac.utils.anticheat.PlayerDataManager;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public enum GrimAPI {
    INSTANCE;

    private final SpectateManager spectateManager = new SpectateManager();
    private final PlayerDataManager playerDataManager = new PlayerDataManager();
    private final InitManager initManager = new InitManager();
    private final TickManager tickManager = new TickManager();
    private final DiscordManager discordManager = new DiscordManager();

    private ConfigManager configManager;
    private AlertManager alertManager;
    private JavaPlugin plugin;

    public void load(final JavaPlugin plugin) {
        this.plugin = plugin;
        this.configManager = new ConfigManager();
        this.alertManager = new AlertManager();
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

    public JavaPlugin getPlugin() {
        return plugin;
    }
}
