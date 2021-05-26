package ac.grim.grimac;

import ac.grim.grimac.checks.predictionengine.MovementCheckRunner;
import ac.grim.grimac.events.bukkit.FlatPlayerBlockBreakPlace;
import ac.grim.grimac.events.bukkit.MagicPlayerBlockBreakPlace;
import ac.grim.grimac.events.bukkit.PistonEvent;
import ac.grim.grimac.events.bukkit.PlayerJoinQuitListener;
import ac.grim.grimac.events.packets.*;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.PlayerFlyingData;
import ac.grim.grimac.utils.data.PredictionData;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.packetwrappers.play.out.transaction.WrappedPacketOutTransaction;
import io.github.retrooper.packetevents.settings.PacketEventsSettings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class GrimAC extends JavaPlugin {
    public static ConcurrentHashMap<Player, GrimPlayer> playerGrimHashMap = new ConcurrentHashMap<>();
    public static Plugin plugin;
    private static int currentTick = 0;
    private ScheduledExecutorService transactionSender;

    public static int getCurrentTick() {
        return currentTick;
    }

    @Override
    public void onLoad() {
        PacketEvents.create(this);
        PacketEventsSettings settings = PacketEvents.get().getSettings();
        settings.checkForUpdates(false).bStats(true);
        PacketEvents.get().loadAsyncNewThread();
    }

    @Override
    public void onDisable() {
        transactionSender.shutdownNow();
        PacketEvents.get().terminate();
    }

    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(new PlayerJoinQuitListener(), this);

        if (XMaterial.isNewVersion()) {
            Bukkit.getPluginManager().registerEvents(new FlatPlayerBlockBreakPlace(), this);
        } else {
            Bukkit.getPluginManager().registerEvents(new MagicPlayerBlockBreakPlace(), this);
        }

        Bukkit.getPluginManager().registerEvents(new PistonEvent(), this);
    }

    // Don't add online players - exempt the players on reload due to chunk caching system
    @Override
    public void onEnable() {
        plugin = this;

        registerEvents();
        registerPackets();
        registerChecks();
        scheduleTransactionPacketSend();

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            currentTick++;

            while (true) {
                PredictionData data = MovementCheckRunner.waitingOnServerQueue.poll();

                if (data == null) break;

                MovementCheckRunner.executor.submit(() -> MovementCheckRunner.check(data));
            }

            for (GrimPlayer player : GrimAC.playerGrimHashMap.values()) {
                player.playerFlyingQueue.add(new PlayerFlyingData(currentTick, player.bukkitPlayer.isFlying()));
            }
        }, 0, 1);
    }

    public void registerPackets() {
        PacketEvents.get().registerListener(new PacketPositionListener());
        PacketEvents.get().registerListener(new PacketPlayerAbilities());
        PacketEvents.get().registerListener(new PacketPlayerVelocity());
        PacketEvents.get().registerListener(new PacketPingListener());
        PacketEvents.get().registerListener(new PacketEntityAction());
        PacketEvents.get().registerListener(new PacketEntityReplication());

        PacketEvents.get().registerListener(new PacketFireworkListener());
        PacketEvents.get().registerListener(new PacketPlayerTeleport());

        PacketEvents.get().registerListener(new PacketMountVehicle());

        try {
            PacketEvents.get().registerListener(new PacketWorldReader());
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            getLogger().severe("The async world reader has broke! Panic and report this error!");
            getLogger().severe("// TODO: Fall back to just reading the world directly");
            exception.printStackTrace();
        }


        PacketEvents.get().init();
    }

    public void registerChecks() {
        //GenericMovementCheck.registerCheck(new MovementVelocityCheck());
        //GenericMovementCheck.registerCheck(new Timer());
    }


    // We are doing this on another thread to try and stop any desync
    // Garbage collection can still affect this, although gc shouldn't be more than 100 ms.
    // On my own server, the average gc is 80.95 ms, without any old gen
    // Probably "close enough" if we average the 5 most recent transactions
    // Even at 10 tps, we still will send 20 times a second
    public void scheduleTransactionPacketSend() {
        transactionSender = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setDaemon(true).build());
        transactionSender.scheduleAtFixedRate(() -> {

            for (GrimPlayer player : GrimAC.playerGrimHashMap.values()) {
                short packetID = player.getNextTransactionID();
                try {
                    PacketEvents.get().getPlayerUtils().sendPacket(player.bukkitPlayer, new WrappedPacketOutTransaction(0, packetID, false));
                    // Get current time for every player just in cause of pauses
                    player.transactionsSent.put(packetID, System.currentTimeMillis());
                } catch (Exception e) {
                    GrimAC.plugin.getLogger().warning("Error sending transaction packet, did the player log out?");
                }
            }
        }, 50, 50, TimeUnit.MILLISECONDS);
    }
}
