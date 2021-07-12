package ac.grim.grimac;

import ac.grim.grimac.events.bukkit.*;
import ac.grim.grimac.events.packets.*;
import ac.grim.grimac.events.packets.worldreader.*;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.MovementCheckRunner;
import ac.grim.grimac.utils.data.PredictionData;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.settings.PacketEventsSettings;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class GrimAC extends JavaPlugin {
    public static ConcurrentHashMap<Player, GrimPlayer> playerGrimHashMap = new ConcurrentHashMap<>();
    private static Plugin plugin;
    // For syncing together the anticheat and main thread
    private static int currentTick = 0;

    public static int getCurrentTick() {
        return currentTick;
    }

    public static InputStream staticGetResource(String resourceName) {
        return plugin.getResource(resourceName);
    }

    public static Logger staticGetLogger() {
        return plugin.getLogger();
    }

    @Override
    public void onLoad() {
        PacketEvents.create(this);
        PacketEventsSettings settings = PacketEvents.get().getSettings();
        settings.fallbackServerVersion(ServerVersion.v_1_7_10).compatInjector(false).checkForUpdates(false).bStats(true);
        PacketEvents.get().loadAsyncNewThread();
    }

    @Override
    public void onDisable() {
        PacketEvents.get().terminate();
    }

    // Don't add online players - exempt the players on reload by not adding them to hashmap due to chunk caching system
    @Override
    public void onEnable() {
        plugin = this;

        registerEvents();
        registerPackets();

        // Try and sync together the main thread with packet threads - this is really difficult without a good solution
        // This works as schedulers run at the beginning of the tick
        // Sync to make sure we loop all players before any events and because this is very fast.
        // It does show up on spark which is sad, but oh well.
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            currentTick++;

            for (GrimPlayer player : GrimAC.playerGrimHashMap.values()) {
                player.lastTransactionAtStartOfTick = player.packetStateData.packetLastTransactionReceived;
            }
        }, 0, 1);

        // Place tasks that were waiting on the server tick to "catch up" back into the queue
        // Async because there is no reason to do this sync
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            while (true) {
                PredictionData data = MovementCheckRunner.waitingOnServerQueue.poll();

                if (data == null) break;

                MovementCheckRunner.executor.runCheck(data);
            }
        }, 0, 1);

        // Scale number of threads for the anticheat every second
        // And anyways, it doesn't consume much performance
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            // Set number of threads one per every 20 players, rounded up
            int targetThreads = (Bukkit.getOnlinePlayers().size() / 20) + 1;
            if (MovementCheckRunner.executor.getPoolSize() != targetThreads) {
                MovementCheckRunner.executor.setMaximumPoolSize(targetThreads);
            }
        }, 20, 100);

        // Writing packets takes more time than it appears
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            for (GrimPlayer player : GrimAC.playerGrimHashMap.values()) {
                player.sendTransactionOrPingPong();
            }
        }, 1, 1);
    }

    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(new PlayerJoinQuitListener(), this);

        if (XMaterial.isNewVersion()) {
            Bukkit.getPluginManager().registerEvents(new FlatPlayerBlockBreakPlace(), this);
        } else {
            Bukkit.getPluginManager().registerEvents(new MagicPlayerBlockBreakPlace(), this);
        }

        if (XMaterial.supports(9)) {
            Bukkit.getPluginManager().registerEvents(new PlayerToggleElytra(), this);
        }

        if (XMaterial.supports(13)) {
            Bukkit.getPluginManager().registerEvents(new RiptideEvent(), this);
        }

        Bukkit.getPluginManager().registerEvents(new PistonEvent(), this);
    }

    public void registerPackets() {
        PacketEvents.get().registerListener(new PacketPositionListener());
        PacketEvents.get().registerListener(new PacketVehicleMoves());
        PacketEvents.get().registerListener(new PacketPlayerAbilities());
        PacketEvents.get().registerListener(new PacketPlayerVelocity());
        PacketEvents.get().registerListener(new PacketPingListener());
        PacketEvents.get().registerListener(new PacketPlayerDigging());
        PacketEvents.get().registerListener(new PacketPlayerAttack());
        PacketEvents.get().registerListener(new PacketEntityAction());
        PacketEvents.get().registerListener(new PacketEntityReplication());
        PacketEvents.get().registerListener(new PacketBlockAction());

        PacketEvents.get().registerListener(new PacketFireworkListener());
        PacketEvents.get().registerListener(new PacketElytraListener());
        PacketEvents.get().registerListener(new PacketPlayerTeleport());

        if (XMaterial.getVersion() >= 17) {
            PacketEvents.get().registerListener(new PacketWorldReaderSeventeen());
        } else if (XMaterial.getVersion() == 16) {
            PacketEvents.get().registerListener(new PacketWorldReaderSixteen());
        } else if (XMaterial.isNewVersion()) {
            PacketEvents.get().registerListener(new PacketWorldReaderThirteen());
        } else if (XMaterial.getVersion() > 8) {
            PacketEvents.get().registerListener(new PacketWorldReaderNine());
        } else if (XMaterial.getVersion() == 8) {
            PacketEvents.get().registerListener(new PacketWorldReaderEight());
        } else {
            PacketEvents.get().registerListener(new PacketWorldReaderSeven());
        }

        PacketEvents.get().init();
    }
}
