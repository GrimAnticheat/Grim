package ac.grim.grimac;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.MovementCheckRunner;
import ac.grim.grimac.utils.data.PredictionData;
import ac.grim.grimac.utils.latency.CompensatedWorld;
import ac.grim.grimac.utils.latency.CompensatedWorldFlat;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.settings.PacketEventsSettings;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.util.logging.Logger;

public final class GrimAC extends JavaPlugin {
    public static Plugin plugin;
    // For syncing together the anticheat and main thread
    private static final int currentTick = 0;

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
        plugin = this;

        // Reading the palette takes a while, do it first
        if (XMaterial.isNewVersion())
            CompensatedWorldFlat.init();
        CompensatedWorld.init();

        PacketEvents.create(this);
        PacketEventsSettings settings = PacketEvents.get().getSettings();
        settings.fallbackServerVersion(ServerVersion.v_1_7_10).compatInjector(false).checkForUpdates(false).bStats(true);
        PacketEvents.get().loadAsyncNewThread();
    }

    @Override
    public void onDisable() {
        GrimAPI.INSTANCE.stop(this);
        PacketEvents.get().terminate();
    }

    // Don't add online players - exempt the players on reload by not adding them to hashmap due to chunk caching system
    @Override
    public void onEnable() {
        GrimAPI.INSTANCE.start(this);

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

        // Writing packets takes more time than it appears - don't flush to try and get the packet to send right before
        // the server begins sending packets to the client
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
                player.sendTransactionOrPingPong(player.getNextTransactionID(1), true);
            }
        }, 1, 1);
    }
}
