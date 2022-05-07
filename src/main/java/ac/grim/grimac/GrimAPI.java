package ac.grim.grimac;

import ac.grim.grimac.manager.*;
import ac.grim.grimac.utils.anticheat.PlayerDataManager;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public enum GrimAPI {
    INSTANCE;

    private final AlertManager alertManager = new AlertManager();
    private final PlayerDataManager playerDataManager = new PlayerDataManager();
    private final InitManager initManager = new InitManager();
    private final TickManager tickManager = new TickManager();
    private final DiscordManager discordManager = new DiscordManager();

    private ConfigManager configManager;
    private JavaPlugin plugin;

    public void load(final JavaPlugin plugin) {
        this.plugin = plugin;
        this.configManager = new ConfigManager();
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
