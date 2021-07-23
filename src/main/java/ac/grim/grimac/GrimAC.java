package ac.grim.grimac;

import ac.grim.grimac.events.bukkit.*;
import ac.grim.grimac.events.packets.*;
import ac.grim.grimac.events.packets.worldreader.*;
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
import org.bukkit.ChatColor;
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
        PacketEvents.get().terminate();
    }

    // Don't add online players - exempt the players on reload by not adding them to hashmap due to chunk caching system
    @Override
    public void onEnable() {
        registerEvents();
        registerPackets();

        // Try and sync together the main thread with packet threads - this is really difficult without a good solution
        // This works as schedulers run at the beginning of the tick
        // Sync to make sure we loop all players before any events and because this is very fast.
        // It does show up on spark which is sad, but oh well.
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            currentTick++;

            for (GrimPlayer player : GrimAC.playerGrimHashMap.values()) {
                player.lastTransactionAtStartOfTick = player.packetStateData.packetLastTransactionReceived.get();
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

        if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17)) {
            // Enable ping -> transaction packet
            System.setProperty("com.viaversion.handlePingsAsInvAcknowledgements", "true");

            // Check if we support this property
            try {
                Plugin viaBackwards = Bukkit.getPluginManager().getPlugin("ViaBackwards");
                if (viaBackwards != null) {
                    String[] split = viaBackwards.getDescription().getVersion().replace("-SNAPSHOT", "").split("\\.");

                    if (split.length == 3) {
                        // If the version is before 4.0.2
                        if (Integer.parseInt(split[0]) < 4 || (Integer.parseInt(split[1]) == 0 && Integer.parseInt(split[2]) < 2)) {
                            getLogger().warning(ChatColor.RED + "Please update ViaBackwards to 4.0.2 or newer");
                            getLogger().warning(ChatColor.RED + "An important packet is broken for 1.16 and below clients on this ViaBackwards version");
                            getLogger().warning(ChatColor.RED + "Disabling all checks for 1.16 and below players as otherwise they WILL be falsely banned");
                            getLogger().warning(ChatColor.RED + "Supported version: " + ChatColor.WHITE + "https://github.com/ViaVersion/ViaBackwards/actions/runs/1039987269");

                            PlayerJoinQuitListener.isViaLegacyUpdated = false;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
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

        Bukkit.getPluginManager().registerEvents(new PlayerConsumeItem(), this);
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
        PacketEvents.get().registerListener(new PacketSelfMetadataListener());
        PacketEvents.get().registerListener(new PacketPlayerTeleport());

        if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17)) {
            PacketEvents.get().registerListener(new PacketWorldReaderSeventeen());
        } else if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_16)) {
            PacketEvents.get().registerListener(new PacketWorldReaderSixteen());
        } else if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_13)) {
            PacketEvents.get().registerListener(new PacketWorldReaderThirteen());
        } else if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_9)) {
            PacketEvents.get().registerListener(new PacketWorldReaderNine());
        } else if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_8)) {
            PacketEvents.get().registerListener(new PacketWorldReaderEight());
        } else {
            PacketEvents.get().registerListener(new PacketWorldReaderSeven());
        }

        PacketEvents.get().init();
    }
}
