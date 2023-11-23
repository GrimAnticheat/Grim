package ac.grim.grimac;

import ac.grim.grimac.manager.*;
import ac.grim.grimac.utils.anticheat.PlayerDataManager;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public enum GrimAPI {
    INSTANCE;

    private final AlertManager alertManager = new AlertManager();
    private final SpectateManager spectateManager = new SpectateManager();
    private final DiscordManager discordManager = new DiscordManager();
    private final PlayerDataManager playerDataManager = new PlayerDataManager();
    private final TickManager tickManager = new TickManager();
    private final GrimExternalAPI externalAPI = new GrimExternalAPI(this);
    private InitManager initManager;
    private ConfigManager configManager;
    private JavaPlugin plugin;

    public void load(final JavaPlugin plugin) {
        this.plugin = plugin;
        this.configManager = new ConfigManager();
        initManager = new InitManager();
        initManager.load();

        final Package grimPackage = GrimAC.class.getPackage();
        if (grimPackage == null) return;

        try (ScanResult scanResult = new ClassGraph()
                .enableAllInfo()
                .acceptPackages(grimPackage.getName())
                .scan()) {

            for (ClassInfo classInfo : scanResult.getAllClasses()) {
                try {
                    Class.forName(classInfo.getName());
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void start(final JavaPlugin plugin) {
        this.plugin = plugin;
        initManager.start();
        Bukkit.getServicesManager().register(GrimAbstractAPI.class, externalAPI, plugin, ServicePriority.Normal);
    }

    public void stop(final JavaPlugin plugin) {
        this.plugin = plugin;
        initManager.stop();
    }
}
