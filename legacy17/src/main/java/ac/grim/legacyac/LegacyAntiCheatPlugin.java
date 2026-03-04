package ac.grim.legacyac;

import ac.grim.legacyac.check.CheckManager;
import ac.grim.legacyac.command.LegacyCommand;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.NetworkTapManager;
import ac.grim.legacyac.network.ProtocolLibBridgeManager;
import ac.grim.legacyac.network.TransactionSyncManager;
import ac.grim.legacyac.network.frame.MovementFrameDispatcher;
import ac.grim.legacyac.regression.RegressionGatekeeper;
import ac.grim.legacyac.regression.RegressionReport;
import ac.grim.legacyac.regression.ViolationLedger;
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
    private ViolationLedger violationLedger;
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
        violationLedger = new ViolationLedger(
                getConfig().getInt("regression.window-size", 512));
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
        getLogger().info("当前启用的移动检查执行路径: " + checkManager.describeMovementExecutionPath());
        getLogger().info("移动检查执行拓扑: " + checkManager.describeMovementPipelineTopology());
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
        // Phase E: Generate regression report on shutdown
        if (violationLedger != null) {
            RegressionReport report = violationLedger.generateReport();
            getLogger().info(report.toReport());

            // Evaluate gates
            RegressionGatekeeper gate = new RegressionGatekeeper(
                    getConfig().getDouble("regression.max-fp-per-check", 0.10D),
                    getConfig().getDouble("regression.max-fp-overall", 0.05D),
                    getConfig().getLong("regression.max-trigger-latency-ms", 200L),
                    getConfig().getLong("regression.min-flags-for-eval", 10L));
            RegressionGatekeeper.GateResult gateResult = gate.evaluate(report);
            getLogger().info(gateResult.toReport());
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

    public ViolationLedger ledger() {
        return violationLedger;
    }

    public double[] resolveEntityBox(Entity entity) {
        if (protocolLibBridgeManager != null) {
            return protocolLibBridgeManager.resolveEntityBox(entity);
        }
        return new double[] { 0.6D, 1.8D };
    }

    public MovementFrameDispatcher movementFrames() {
        return movementFrameDispatcher;
    }

    public TransactionSyncManager transactionSync() {
        return transactionSyncManager;
    }

    public boolean isPacketPipelineActive() {
        return packetPipelineActive;
    }
}
