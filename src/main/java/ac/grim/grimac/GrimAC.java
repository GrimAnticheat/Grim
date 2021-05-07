package ac.grim.grimac;

import ac.grim.grimac.checks.predictionengine.MovementCheckRunner;
import ac.grim.grimac.events.bukkit.PlayerLagback;
import ac.grim.grimac.events.bukkit.PlayerQuitListener;
import ac.grim.grimac.events.packets.*;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.latency.CompensatedWorld;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.packetwrappers.play.out.transaction.WrappedPacketOutTransaction;
import io.github.retrooper.packetevents.settings.PacketEventsSettings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class GrimAC extends JavaPlugin {
    public static ConcurrentHashMap<Player, GrimPlayer> playerGrimHashMap = new ConcurrentHashMap<>();
    public static Plugin plugin;
    public static AtomicInteger currentTick = new AtomicInteger(0);
    public static Long lastReload = 0L;
    ScheduledExecutorService transactionSender;

    @Override
    public void onLoad() {
        PacketEvents.create(this);
        PacketEventsSettings settings = PacketEvents.get().getSettings();
        settings.checkForUpdates(false).compatInjector(false);
        PacketEvents.get().loadAsyncNewThread();
    }

    @Override
    public void onDisable() {
        transactionSender.shutdownNow();
        PacketEvents.get().terminate();
    }

    @Override
    public void onEnable() {
        plugin = this;

        registerEvents();
        registerPackets();
        registerChecks();
        CompensatedWorld.initBlockID();
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
        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerLagback(), this);
        Bukkit.getPluginManager().registerEvents(new MovementCheckRunner(), this);
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
        PacketEvents.get().registerListener(new PacketPlayerJoin());
        PacketEvents.get().registerListener(new PacketVehicleMoveClientbound());
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

    public void handleReload() {
        if (Bukkit.getOnlinePlayers().size() == 0) return;

        lastReload = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            playerGrimHashMap.put(player, new GrimPlayer(player));
            MovementCheckRunner.queuedPredictions.put(player.getUniqueId(), new ConcurrentLinkedQueue<>());
        }

        // TODO: Remove this hack
        /*World world = Bukkit.getWorlds().get(0);
        WorldServer craftWorld = ((CraftWorld) world).getHandle();

        for (Chunk chunk : world.getLoadedChunks()) {
            com.github.steveice10.mc.protocol.data.game.chunk.Chunk[] chunks = new com.github.steveice10.mc.protocol.data.game.chunk.Chunk[16];
            Column section = new Column(chunk.getX(), chunk.getZ(), chunks);

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < 256; y++) {
                        int columnNum = Math.floorDiv(y, 16);
                        IBlockData blockID = craftWorld.getType(new BlockPosition(chunk.getX() << 4 + x, y, chunk.getZ() << 4 + z));
                        if (blockID.getBlock() instanceof BlockAir) continue;

                        if (chunks[columnNum] == null)
                            chunks[columnNum] = new com.github.steveice10.mc.protocol.data.game.chunk.Chunk();

                        chunks[columnNum].set(x, y % 16, z, Block.getCombinedId(blockID));
                    }
                }
            }

            ChunkCache.addToCache(section, chunk.getX(), chunk.getZ());
        }*/
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
