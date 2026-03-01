package ac.grim.legacyac;

import ac.grim.legacyac.check.CheckManager;
import ac.grim.legacyac.command.LegacyCommand;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.NetworkTapManager;
import ac.grim.legacyac.network.ProtocolLibBridgeManager;
import ac.grim.legacyac.network.TransactionSyncManager;
import ac.grim.legacyac.network.frame.MovementFrameDispatcher;
import ac.grim.legacyac.util.AlertManager;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class LegacyAntiCheatPlugin extends JavaPlugin {
    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<UUID, PlayerData>();
    private AlertManager alertManager;
    private CheckManager checkManager;
    private NetworkTapManager networkTapManager;
    private ProtocolLibBridgeManager protocolLibBridgeManager;
    private TransactionSyncManager transactionSyncManager;
    private MovementFrameDispatcher movementFrameDispatcher;
    private boolean packetPipelineActive;
    private int tickTaskId = -1;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        alertManager = new AlertManager(this);
        checkManager = new CheckManager(this);
        getServer().getPluginManager().registerEvents(checkManager, this);
        protocolLibBridgeManager = new ProtocolLibBridgeManager(this);
        boolean protocolActive = protocolLibBridgeManager.start();
        packetPipelineActive = protocolActive;
        movementFrameDispatcher = new MovementFrameDispatcher(this);
        if (!protocolActive) {
            networkTapManager = new NetworkTapManager(this);
            getServer().getPluginManager().registerEvents(networkTapManager, this);
        }
        getCommand("glac").setExecutor(new LegacyCommand(this));
        transactionSyncManager = new TransactionSyncManager(this);
        transactionSyncManager.start();

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
        if (networkTapManager != null) {
            networkTapManager.shutdown();
        }
        if (protocolLibBridgeManager != null) {
            protocolLibBridgeManager.stop();
        }
        if (transactionSyncManager != null) {
            transactionSyncManager.stop();
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
        PlayerData removed = playerDataMap.remove(player.getUniqueId());
        if (removed != null) {
            removed.clearPendingTransactions();
        }
    }

    public AlertManager alerts() {
        return alertManager;
    }

    public CheckManager checks() {
        return checkManager;
    }

    public double[] resolveEntityBox(Entity entity) {
        if (protocolLibBridgeManager != null) {
            return protocolLibBridgeManager.resolveEntityBox(entity);
        }
        return new double[] {0.6D, 1.8D};
    }

    public MovementFrameDispatcher movementFrames() {
        return movementFrameDispatcher;
    }

    public boolean isPacketPipelineActive() {
        return packetPipelineActive;
    }
}
