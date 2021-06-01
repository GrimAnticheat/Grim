package ac.grim.grimac;

import ac.grim.grimac.events.bukkit.FlatPlayerBlockBreakPlace;
import ac.grim.grimac.events.bukkit.MagicPlayerBlockBreakPlace;
import ac.grim.grimac.events.bukkit.PistonEvent;
import ac.grim.grimac.events.bukkit.PlayerJoinQuitListener;
import ac.grim.grimac.events.packets.*;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.MovementCheckRunner;
import ac.grim.grimac.utils.data.PlayerFlyingData;
import ac.grim.grimac.utils.data.PredictionData;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.packetwrappers.play.out.transaction.WrappedPacketOutTransaction;
import io.github.retrooper.packetevents.settings.PacketEventsSettings;
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
        settings.checkForUpdates(false).bStats(true);
        PacketEvents.get().loadAsyncNewThread();
    }

    @Override
    public void onDisable() {
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

    public void registerPackets() {
        PacketEvents.get().registerListener(new PacketPositionListener());
        PacketEvents.get().registerListener(new PacketPlayerAbilities());
        PacketEvents.get().registerListener(new PacketPlayerVelocity());
        PacketEvents.get().registerListener(new PacketPingListener());
        PacketEvents.get().registerListener(new PacketPlayerAttack());
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

    // Don't add online players - exempt the players on reload by not adding them to hashmap due to chunk caching system
    @Override
    public void onEnable() {
        plugin = this;

        registerEvents();
        registerPackets();

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            currentTick++;

            while (true) {
                PredictionData data = MovementCheckRunner.waitingOnServerQueue.poll();

                if (data == null) break;

                MovementCheckRunner.executor.submit(() -> MovementCheckRunner.check(data));
            }

            for (GrimPlayer player : GrimAC.playerGrimHashMap.values()) {
                player.playerFlyingQueue.add(new PlayerFlyingData(currentTick, player.bukkitPlayer.isFlying()));
                sendTransaction(player.getNextTransactionID(), player);
            }
        }, 0, 1);

        // Scale number of threads for the anticheat every second
        // Could be higher but a large number of players joining at once could be bad
        // And anyways, it doesn't consume much performance
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            // Set number of threads one per every 20 players, rounded up
            int targetThreads = (Bukkit.getOnlinePlayers().size() / 20) + 1;
            if (MovementCheckRunner.executor.getPoolSize() != targetThreads) {
                MovementCheckRunner.executor.setMaximumPoolSize(targetThreads);
            }
        }, 20, 20);
    }

    // Shouldn't error, but be on the safe side as this is networking stuff
    private void sendTransaction(short transactionID, GrimPlayer player) {
        try {
            PacketEvents.get().getPlayerUtils().sendPacket(player.bukkitPlayer, new WrappedPacketOutTransaction(0, transactionID, false));
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
