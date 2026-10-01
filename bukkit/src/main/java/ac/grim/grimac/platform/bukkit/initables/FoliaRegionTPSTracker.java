package ac.grim.grimac.platform.bukkit.initables;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.start.StartableInitable;
import ac.grim.grimac.manager.init.stop.StoppableInitable;
import ac.grim.grimac.platform.api.Platform;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.api.scheduler.TaskHandle;
import ac.grim.grimac.platform.bukkit.GrimACBukkitLoaderPlugin;
import ac.grim.grimac.platform.bukkit.utils.reflection.FoliaRegionTPS;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FoliaRegionTPSTracker implements StartableInitable, StoppableInitable, Listener {

    private static final long UPDATE_PERIOD_TICKS = 20L;
    private static final Map<UUID, Entry> ENTRIES = new ConcurrentHashMap<>();

    public static double getTPS(UUID uuid) {
        if (uuid == null) return Double.NaN;
        Entry entry = ENTRIES.get(uuid);
        return entry == null ? Double.NaN : entry.tps;
    }

    @Override
    public void start() {
        if (GrimAPI.INSTANCE.getPlatform() != Platform.FOLIA || !FoliaRegionTPS.isSupported()) return;

        Bukkit.getPluginManager().registerEvents(this, GrimACBukkitLoaderPlugin.LOADER);
        for (Player player : Bukkit.getOnlinePlayers()) {
            track(player);
        }
    }

    @Override
    public void stop() {
        HandlerList.unregisterAll(this);
        for (Entry entry : ENTRIES.values()) {
            entry.cancel();
        }
        ENTRIES.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        track(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        untrack(event.getPlayer().getUniqueId());
    }

    private void track(Player player) {
        UUID uuid = player.getUniqueId();
        Entry entry = new Entry();

        Entry previous = ENTRIES.put(uuid, entry);
        if (previous != null) previous.cancel();

        PlatformPlayer platformPlayer = GrimAPI.INSTANCE.getPlatformPlayerFactory().getFromNativePlayerType(player);
        TaskHandle handle = GrimAPI.INSTANCE.getScheduler().getEntityScheduler().runAtFixedRate(
                platformPlayer,
                GrimAPI.INSTANCE.getGrimPlugin(),
                () -> {
                    double tps = FoliaRegionTPS.regionTPS(player.getLocation());
                    if (Double.isFinite(tps)) entry.tps = tps;
                },
                () -> ENTRIES.remove(uuid, entry),
                1L,
                UPDATE_PERIOD_TICKS
        );

        if (handle == null) {
            ENTRIES.remove(uuid, entry);
            return;
        }

        entry.handle = handle;
        if (ENTRIES.get(uuid) != entry) {
            handle.cancel();
        }
    }

    private void untrack(UUID uuid) {
        Entry entry = ENTRIES.remove(uuid);
        if (entry != null) entry.cancel();
    }

    private static final class Entry {
        private volatile double tps = Double.NaN;
        private volatile TaskHandle handle;

        private void cancel() {
            TaskHandle current = handle;
            if (current != null) current.cancel();
        }
    }
}
