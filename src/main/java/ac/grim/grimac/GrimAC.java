package ac.grim.grimac;

import ac.grim.grimac.checks.movement.MovementCheckRunner;
import ac.grim.grimac.events.bukkit.PlayerJoinLeaveListener;
import ac.grim.grimac.events.bukkit.PlayerLagback;
import ac.grim.grimac.events.bukkit.PlayerVelocityPackets;
import ac.grim.grimac.events.bukkit.TestEvent;
import ac.grim.grimac.events.packets.*;
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
import java.util.concurrent.atomic.AtomicInteger;

public final class GrimAC extends JavaPlugin {
    public static ConcurrentHashMap<Player, GrimPlayer> playerGrimHashMap = new ConcurrentHashMap<>();
    public static Plugin plugin;
    public static AtomicInteger currentTick = new AtomicInteger(-6000);

    @Override
    public void onLoad() {
        PacketEvents.create(this);
        PacketEventsSettings settings = PacketEvents.get().getSettings();
        settings.checkForUpdates(false).compatInjector(false);
        PacketEvents.get().loadAsyncNewThread();
    }

    @Override
    public void onDisable() {
        PacketEvents.get().terminate();
    }

    @Override
    public void onEnable() {
        plugin = this;

        registerEvents();
        registerPackets();
        registerChecks();
        scheduleTransactionPacketSend();
        handleReload();

        // Debug
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                //Bukkit.broadcastMessage("Ping is " + PacketEvents.get().getPlayerUtils().getSmoothedPing(player));
            }
        }, 1, 1);
    }

    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(new PlayerJoinLeaveListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerLagback(), this);
        Bukkit.getPluginManager().registerEvents(new TestEvent(), this);
        Bukkit.getPluginManager().registerEvents(new MovementCheckRunner(), this);
    }

    public void registerPackets() {
        PacketEvents.get().registerListener(new PacketPositionListener());
        PacketEvents.get().registerListener(new PlayerVelocityPackets());
        PacketEvents.get().registerListener(new PacketPingListener());
        PacketEvents.get().registerListener(new PacketEntityAction());
        PacketEvents.get().registerListener(new PacketFireworkListener());

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

    public void handleReload() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerGrimHashMap.put(player, new GrimPlayer(player));
        }
    }

    // We are doing this on another thread to try and stop any desync
    // Garbage collection can still affect this, although gc shouldn't be more than 100 ms.
    // On my own server, the average gc is 80.95 ms, without any old gen
    // Probably "close enough" if we average the 5 most recent transactions
    // Even at 10 tps, we still will send 20 times a second
    public void scheduleTransactionPacketSend() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            short packetID = (short) currentTick.getAndIncrement();

            for (GrimPlayer player : GrimAC.playerGrimHashMap.values()) {
                try {
                    PacketEvents.get().getPlayerUtils().sendPacket(player.bukkitPlayer, new WrappedPacketOutTransaction(0, packetID, false));
                    // Get current time for every player just in cause of pauses
                    player.transactionsSent.put(packetID, System.currentTimeMillis());
                } catch (Exception e) {
                    GrimAC.plugin.getLogger().warning("Error sending transaction packet, did the player log out?");
                }
            }

            // Create a fixed size of handling five minutes worth of transactions
            // Use negative transactions to stop this from touching the server
            currentTick.compareAndSet(-1, -6000);
        }, 50, 50, TimeUnit.MILLISECONDS);
    }
}
