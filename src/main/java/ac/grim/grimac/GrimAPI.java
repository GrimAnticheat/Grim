package ac.grim.grimac;

import ac.grim.grimac.api.GrimAbstractAPI;
import ac.grim.grimac.manager.*;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.PlayerDataManager;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import static java.util.UUID.randomUUID;

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
        User userCannotJoin = new User(
                null, ConnectionState.LOGIN, ClientVersion.UNKNOWN, //Invalid client version
                new UserProfile(randomUUID(), "USER_CANNOT_JOIN")
        );
        new GrimPlayer(userCannotJoin);
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
