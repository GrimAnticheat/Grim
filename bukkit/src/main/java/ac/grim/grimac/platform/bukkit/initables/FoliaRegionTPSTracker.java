package ac.grim.grimac.platform.bukkit.initables;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.start.StartableInitable;
import ac.grim.grimac.platform.api.Platform;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.api.scheduler.TaskHandle;
import ac.grim.grimac.platform.bukkit.GrimACBukkitLoaderPlugin;
import ac.grim.grimac.platform.bukkit.utils.reflection.FoliaRegionTPS;
import ac.grim.grimac.utils.anticheat.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FoliaRegionTPSTracker implements StartableInitable, Listener {

    private static final long UPDATE_PERIOD_TICKS = 20L;
    private static final Map<UUID, Entry> ENTRIES = new ConcurrentHashMap<>();

    public static double getTPS(UUID uuid) {
        Entry entry = ENTRIES.get(uuid);
        return entry == null ? Double.NaN : entry.tps;
    }

    @Override
    public void start() {
        if (GrimAPI.INSTANCE.getPlatform() != Platform.FOLIA) return;

        if (!FoliaRegionTPS.isSupported()) {
            LogUtil.warn("Folia region TPS is unavailable on this server, %tps% will report NaN");
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, GrimACBukkitLoaderPlugin.LOADER);
        for (Player player : Bukkit.getOnlinePlayers()) {
            track(player);
        }
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
                () -> entry.tps = FoliaRegionTPS.currentRegionTPS(),
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
