package ac.grim.legacyac;

import ac.grim.legacyac.check.CheckManager;
import ac.grim.legacyac.command.LegacyCommand;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.util.AlertManager;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class LegacyAntiCheatPlugin extends JavaPlugin {
    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<UUID, PlayerData>();
    private AlertManager alertManager;
    private CheckManager checkManager;
    private int tickTaskId = -1;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        alertManager = new AlertManager(this);
        checkManager = new CheckManager(this);
        getServer().getPluginManager().registerEvents(checkManager, this);
        getCommand("glac").setExecutor(new LegacyCommand(this));

        tickTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                checkManager.tick();
            }
        }, 20L, 20L);

        getLogger().info("GrimLegacyAC enabled with " + checkManager.getCheckCount() + " checks.");
    }

    @Override
    public void onDisable() {
        if (tickTaskId != -1) {
            getServer().getScheduler().cancelTask(tickTaskId);
        }
        playerDataMap.clear();
    }

    public PlayerData getPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData existing = playerDataMap.get(uuid);
        if (existing != null) {
            return existing;
        }
        PlayerData created = new PlayerData(uuid);
        playerDataMap.put(uuid, created);
        return created;
    }

    public void removePlayerData(Player player) {
        playerDataMap.remove(player.getUniqueId());
    }

    public AlertManager alerts() {
        return alertManager;
    }

    public CheckManager checks() {
        return checkManager;
    }
}
